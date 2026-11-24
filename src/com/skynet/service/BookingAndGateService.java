package com.skynet.service;

import com.skynet.enums.FareClass;
import com.skynet.enums.LoyaltyTier;
import com.skynet.enums.TicketStatus;
import com.skynet.exception.FlightOverbookedException;
import com.skynet.exception.InsufficientWalletBalanceException;
import com.skynet.model.Flight;
import com.skynet.model.Passenger;
import com.skynet.model.Ticket;
import com.skynet.repository.InMemoryAirlineStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class BookingAndGateService {
    private static final int REFUND_PERCENT = 90;
    private static final int MAX_ACTIVITY = 8;

    private final InMemoryAirlineStore store;
    private final TicketPriorityComparator comparator = new TicketPriorityComparator();

    private final Map<String, PriorityQueue<Ticket>> standbyLists = new HashMap<>();

    private final Deque<String> recentActivity = new ArrayDeque<>();

    private int nextTicketNumber = 1001;
    private int nextPassengerNumber = 101;
    private volatile int paymentDelayMillis = 0;

    public BookingAndGateService(InMemoryAirlineStore store) {
        this.store = store;
    }

    public synchronized Flight addFlight(String flightId, String source, String destination,
                                         int seats, double baseFare) {
        if (store.findFlight(flightId.trim().toUpperCase()) != null) {
            throw new IllegalArgumentException("Flight " + flightId + " already exists.");
        }
        Flight flight = new Flight(flightId, source, destination, seats, baseFare);
        store.addFlight(flight);
        log("Flight added: " + flight);
        return flight;
    }

    public synchronized Passenger registerPassenger(String name, LoyaltyTier tier, double wallet) {
        Passenger passenger = new Passenger("P" + nextPassengerNumber, name, tier, wallet);
        nextPassengerNumber++;
        store.addPassenger(passenger);
        log("Passenger registered: " + passenger);
        return passenger;
    }

    public synchronized Ticket bookTicket(String passengerId, String flightId, FareClass fareClass)
            throws FlightOverbookedException, InsufficientWalletBalanceException {
        Passenger passenger = getPassenger(passengerId);
        Flight flight = getFlight(flightId);

        if (store.hasActiveTicket(passenger.getPassengerId(), flight.getFlightId())) {
            throw new IllegalStateException(passenger.getName() + " already has a ticket on " + flight.getFlightId());
        }

        double fare = flight.getFare(fareClass);
        if (passenger.getWalletBalance() < fare) {
            throw new InsufficientWalletBalanceException(passenger.getName(), fare, passenger.getWalletBalance());
        }

        pause(paymentDelayMillis);

        Ticket ticket;
        if (flight.reserveSeat()) {
            passenger.debit(fare);
            ticket = new Ticket(nextTicketNumber, passenger, flight, fareClass, fare, TicketStatus.CONFIRMED);
            log(ticket.getTicketId() + " CONFIRMED for " + passenger.getName());
        } else {
            PriorityQueue<Ticket> standby = getStandbyQueue(flight.getFlightId());
            if (standby.size() >= flight.getStandbyLimit()) {
                throw new FlightOverbookedException("Flight " + flight.getFlightId()
                        + " is overbooked: all seats and standby places are taken.");
            }
            ticket = new Ticket(nextTicketNumber, passenger, flight, fareClass, fare, TicketStatus.STANDBY);
            standby.add(ticket);
            log(ticket.getTicketId() + " on STANDBY for " + passenger.getName());
        }
        nextTicketNumber++;
        store.addTicket(ticket);
        return ticket;
    }

    public synchronized String cancelTicket(String ticketId) {
        Ticket ticket = store.findTicket(ticketId.trim().toUpperCase());
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket " + ticketId + " not found.");
        }
        if (!ticket.isActive()) {
            throw new IllegalStateException("Ticket " + ticket.getTicketId() + " is already cancelled.");
        }

        if (ticket.getStatus() == TicketStatus.STANDBY) {
            getStandbyQueue(ticket.getFlight().getFlightId()).remove(ticket);
            ticket.setStatus(TicketStatus.CANCELLED);
            log(ticket.getTicketId() + " standby CANCELLED");
            return ticket.getTicketId() + " (standby) cancelled. Nothing to refund.";
        }

        double refund = ticket.getFare() * REFUND_PERCENT / 100;
        ticket.getPassenger().credit(refund);
        ticket.getFlight().releaseSeat();
        ticket.setStatus(TicketStatus.CANCELLED);
        log(String.format("%s CANCELLED, refund Rs.%,.2f", ticket.getTicketId(), refund));

        String message = String.format("%s cancelled. Refund Rs.%,.2f to %s.",
                ticket.getTicketId(), refund, ticket.getPassenger().getName());
        Ticket promoted = promoteFromStandby(ticket.getFlight());
        if (promoted != null) {
            message = message + "\n  " + promoted.getPassenger().getName() + " (" + promoted.getPassenger().getTier()
                    + ") moved from standby to CONFIRMED (" + promoted.getTicketId() + ").";
        }
        return message;
    }

    private Ticket promoteFromStandby(Flight flight) {
        PriorityQueue<Ticket> standby = getStandbyQueue(flight.getFlightId());
        List<Ticket> skipped = new ArrayList<>();
        Ticket promoted = null;
        while (promoted == null && !standby.isEmpty()) {
            Ticket candidate = standby.poll();
            Passenger passenger = candidate.getPassenger();
            if (passenger.getWalletBalance() >= candidate.getFare() && flight.reserveSeat()) {
                passenger.debit(candidate.getFare());
                candidate.setStatus(TicketStatus.CONFIRMED);
                log(candidate.getTicketId() + " PROMOTED from standby");
                promoted = candidate;
            } else {
                skipped.add(candidate);
            }
        }
        standby.addAll(skipped);
        return promoted;
    }

    public synchronized List<Ticket> getStandbyList(String flightId) {
        List<Ticket> list = new ArrayList<>(getStandbyQueue(getFlight(flightId).getFlightId()));
        Collections.sort(list, comparator);
        return list;
    }

    public synchronized Queue<Ticket> getBoardingQueue(String flightId) {
        List<Ticket> confirmed = store.getTicketsForFlight(getFlight(flightId).getFlightId(), TicketStatus.CONFIRMED);
        Collections.sort(confirmed, comparator);
        return new LinkedList<>(confirmed);
    }

    public synchronized List<Flight> getAllFlights() {
        return store.getAllFlights();
    }

    public synchronized List<Passenger> getAllPassengers() {
        return store.getAllPassengers();
    }

    public synchronized List<Passenger> getRecentPassengers(int count) {
        return store.getRecentPassengers(count);
    }

    public synchronized List<Ticket> getAllTickets() {
        return store.getAllTickets();
    }

    public synchronized List<Ticket> getTicketsForFlight(String flightId, TicketStatus status) {
        return store.getTicketsForFlight(getFlight(flightId).getFlightId(), status);
    }

    public synchronized int getStandbyCount(String flightId) {
        return getStandbyQueue(getFlight(flightId).getFlightId()).size();
    }

    public synchronized List<String> getRecentActivity() {
        return new ArrayList<>(recentActivity);
    }

    public synchronized Flight findFlight(String flightId) {
        return store.findFlight(flightId.trim().toUpperCase());
    }

    public void setPaymentDelay(int millis) {
        paymentDelayMillis = millis;
    }

    private Flight getFlight(String flightId) {
        Flight flight = store.findFlight(flightId.trim().toUpperCase());
        if (flight == null) {
            throw new IllegalArgumentException("Flight " + flightId + " not found.");
        }
        return flight;
    }

    private Passenger getPassenger(String passengerId) {
        Passenger passenger = store.findPassenger(passengerId.trim().toUpperCase());
        if (passenger == null) {
            throw new IllegalArgumentException("Passenger " + passengerId + " not found.");
        }
        return passenger;
    }

    private PriorityQueue<Ticket> getStandbyQueue(String flightId) {
        PriorityQueue<Ticket> queue = standbyLists.get(flightId);
        if (queue == null) {
            queue = new PriorityQueue<>(comparator);
            standbyLists.put(flightId, queue);
        }
        return queue;
    }

    private void log(String message) {
        recentActivity.addFirst(message);
        if (recentActivity.size() > MAX_ACTIVITY) {
            recentActivity.removeLast();
        }
    }

    private void pause(int millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
