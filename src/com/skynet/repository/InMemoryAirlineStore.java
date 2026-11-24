package com.skynet.repository;

import com.skynet.enums.TicketStatus;
import com.skynet.model.Flight;
import com.skynet.model.Passenger;
import com.skynet.model.Ticket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class InMemoryAirlineStore {
    private final Map<String, Passenger> passengers = new HashMap<>();
    private final List<Passenger> passengerList = new ArrayList<>();
    private final Map<String, Flight> flights = new TreeMap<>();
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();

    public void addFlight(Flight flight) {
        flights.put(flight.getFlightId(), flight);
    }

    public Flight findFlight(String flightId) {
        return flights.get(flightId);
    }

    public List<Flight> getAllFlights() {
        return new ArrayList<>(flights.values());
    }

    public void addPassenger(Passenger passenger) {
        passengers.put(passenger.getPassengerId(), passenger);
        passengerList.add(passenger);
    }

    public Passenger findPassenger(String passengerId) {
        return passengers.get(passengerId);
    }

    public int getPassengerCount() {
        return passengerList.size();
    }

    public List<Passenger> getAllPassengers() {
        List<Passenger> result = new ArrayList<>();
        ListIterator<Passenger> it = passengerList.listIterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    public List<Passenger> getRecentPassengers(int count) {
        List<Passenger> result = new ArrayList<>();
        ListIterator<Passenger> it = passengerList.listIterator(passengerList.size());
        while (it.hasPrevious() && result.size() < count) {
            result.add(it.previous());
        }
        return result;
    }

    public void addTicket(Ticket ticket) {
        tickets.put(ticket.getTicketId(), ticket);
    }

    public Ticket findTicket(String ticketId) {
        return tickets.get(ticketId);
    }

    public List<Ticket> getAllTickets() {
        return new ArrayList<>(tickets.values());
    }

    public List<Ticket> getTicketsForFlight(String flightId, TicketStatus status) {
        List<Ticket> result = new ArrayList<>();
        Iterator<Ticket> it = tickets.values().iterator();
        while (it.hasNext()) {
            Ticket ticket = it.next();
            if (ticket.getFlight().getFlightId().equals(flightId) && ticket.getStatus() == status) {
                result.add(ticket);
            }
        }
        return result;
    }

    public boolean hasActiveTicket(String passengerId, String flightId) {
        Iterator<Ticket> it = tickets.values().iterator();
        while (it.hasNext()) {
            Ticket ticket = it.next();
            if (ticket.isActive()
                    && ticket.getPassenger().getPassengerId().equals(passengerId)
                    && ticket.getFlight().getFlightId().equals(flightId)) {
                return true;
            }
        }
        return false;
    }
}
