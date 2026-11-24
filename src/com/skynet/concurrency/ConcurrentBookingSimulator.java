package com.skynet.concurrency;

import com.skynet.enums.FareClass;
import com.skynet.enums.TicketStatus;
import com.skynet.exception.FlightOverbookedException;
import com.skynet.exception.InsufficientWalletBalanceException;
import com.skynet.model.Flight;
import com.skynet.model.Passenger;
import com.skynet.model.Ticket;
import com.skynet.service.BookingAndGateService;

import java.util.ArrayList;
import java.util.List;

public class ConcurrentBookingSimulator implements Runnable {
    private static final int SIM_SEATS = 5;
    private static final int PAYMENT_DELAY_MILLIS = 40;

    private final BookingAndGateService service;
    private final Passenger passenger;
    private final String flightId;
    private final FareClass fareClass;
    private String result = "NOT RUN";

    private static int unsafeSeatsLeft;

    public ConcurrentBookingSimulator(BookingAndGateService service, Passenger passenger,
                                      String flightId, FareClass fareClass) {
        this.service = service;
        this.passenger = passenger;
        this.flightId = flightId;
        this.fareClass = fareClass;
    }

    @Override
    public void run() {
        if (service == null) {
            takeSeatUnsafely();
            return;
        }
        try {
            Ticket ticket = service.bookTicket(passenger.getPassengerId(), flightId, fareClass);
            if (ticket.getStatus() == TicketStatus.CONFIRMED) {
                result = "CONFIRMED " + ticket.getTicketId();
            } else {
                result = "STANDBY   " + ticket.getTicketId();
            }
        } catch (FlightOverbookedException e) {
            result = "REJECTED  flight overbooked";
        } catch (InsufficientWalletBalanceException e) {
            result = "REJECTED  not enough money";
        } catch (IllegalStateException e) {
            result = "REJECTED  duplicate booking";
        }
        System.out.printf("   %-16s -> %s%n", Thread.currentThread().getName(), result);
    }

    private void takeSeatUnsafely() {
        if (unsafeSeatsLeft > 0) {
            pause(20);
            unsafeSeatsLeft = unsafeSeatsLeft - 1;
            result = "GOT SEAT";
        } else {
            result = "NO SEAT";
        }
    }

    private static void runUnsafePart(List<Passenger> passengers) throws InterruptedException {
        System.out.println("\n--- PART 1: WITHOUT synchronization (" + SIM_SEATS + " seats, "
                + passengers.size() + " threads) ---");
        unsafeSeatsLeft = SIM_SEATS;
        List<ConcurrentBookingSimulator> tasks = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (Passenger p : passengers) {
            ConcurrentBookingSimulator task = new ConcurrentBookingSimulator(null, p, null, null);
            tasks.add(task);
            threads.add(new Thread(task, "Unsafe-" + firstName(p)));
        }
        startAll(threads);
        joinAll(threads);

        int seatsGiven = 0;
        for (ConcurrentBookingSimulator task : tasks) {
            if (task.result.equals("GOT SEAT")) {
                seatsGiven++;
            }
        }
        System.out.println("   Seats given: " + seatsGiven + " on a " + SIM_SEATS + "-seat flight. Seats left counter: "
                + unsafeSeatsLeft);
        if (seatsGiven > SIM_SEATS) {
            System.out.println("   RACE CONDITION: the flight was overbooked!");
        } else {
            System.out.println("   The race did not show this time. Run it again.");
        }
    }

    private static void runSafePart(BookingAndGateService service, Flight flight, List<Passenger> passengers)
            throws InterruptedException {
        System.out.println("\n--- PART 2: WITH synchronized bookTicket() on " + flight + " ---");
        List<Thread> threads = new ArrayList<>();
        FareClass[] classes = FareClass.values();
        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            FareClass fareClass = classes[i % classes.length];
            threads.add(new Thread(new ConcurrentBookingSimulator(service, p, flight.getFlightId(), fareClass),
                    "Booker-" + firstName(p)));
        }

        Passenger first = passengers.get(0);
        threads.add(new Thread(new ConcurrentBookingSimulator(service, first, flight.getFlightId(), FareClass.ECONOMY),
                "Booker-" + firstName(first) + "-2"));

        service.setPaymentDelay(PAYMENT_DELAY_MILLIS);
        try {
            startAll(threads);
            Thread.sleep(20);
            int blocked = 0;
            for (Thread t : threads) {
                if (t.getState() == Thread.State.BLOCKED) {
                    blocked++;
                }
            }
            System.out.println("   Snapshot: " + blocked + " of " + threads.size()
                    + " threads are BLOCKED waiting for the service lock.");
            joinAll(threads);
        } finally {
            service.setPaymentDelay(0);
        }

        int confirmed = service.getTicketsForFlight(flight.getFlightId(), TicketStatus.CONFIRMED).size();
        int standby = service.getStandbyCount(flight.getFlightId());
        System.out.println("\n   Confirmed: " + confirmed + ", standby: " + standby
                + ", free seats: " + flight.getAvailableSeats());
        if (confirmed <= flight.getTotalSeats() && flight.getAvailableSeats() >= 0) {
            System.out.println("   CHECK PASSED: never more confirmed tickets than seats.");
        } else {
            System.out.println("   CHECK FAILED");
        }
    }

    public static void runSimulation(BookingAndGateService service, List<Passenger> passengers)
            throws InterruptedException {
        Flight flight = service.addFlight(nextFreeFlightId(service), "Delhi", "Mumbai", SIM_SEATS, 4500);
        System.out.println("\n===== CONCURRENT BOOKING SIMULATION =====");
        runUnsafePart(passengers);
        runSafePart(service, flight, passengers);
    }

    private static String nextFreeFlightId(BookingAndGateService service) {
        int number = 901;
        while (service.findFlight("SN" + number) != null) {
            number++;
        }
        return "SN" + number;
    }

    private static void startAll(List<Thread> threads) {
        for (Thread t : threads) {
            t.start();
        }
    }

    private static void joinAll(List<Thread> threads) throws InterruptedException {
        for (Thread t : threads) {
            t.join();
        }
    }

    private static String firstName(Passenger p) {
        return p.getName().split(" ")[0];
    }

    private static void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
