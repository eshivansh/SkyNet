package com.skynet.model;

import com.skynet.enums.FareClass;
import com.skynet.enums.TicketStatus;

import java.time.LocalTime;

public class Ticket implements Comparable<Ticket> {
    private final String ticketId;
    private final int ticketNumber;
    private final Passenger passenger;
    private final Flight flight;
    private final FareClass fareClass;
    private final double fare;
    private final LocalTime bookedAt;
    private TicketStatus status;

    public Ticket(int ticketNumber, Passenger passenger, Flight flight, FareClass fareClass,
                  double fare, TicketStatus status) {
        this.ticketId = "TKT-" + ticketNumber;
        this.ticketNumber = ticketNumber;
        this.passenger = passenger;
        this.flight = flight;
        this.fareClass = fareClass;
        this.fare = fare;
        this.status = status;
        this.bookedAt = LocalTime.now();
    }

    @Override
    public int compareTo(Ticket other) {
        int result = this.bookedAt.compareTo(other.bookedAt);
        if (result == 0) {
            result = Integer.compare(this.ticketNumber, other.ticketNumber);
        }
        return result;
    }

    public boolean isActive() {
        return status != TicketStatus.CANCELLED;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Flight getFlight() {
        return flight;
    }

    public FareClass getFareClass() {
        return fareClass;
    }

    public double getFare() {
        return fare;
    }

    public LocalTime getBookedAt() {
        return bookedAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return ticketId + " " + passenger.getName() + " " + flight.getFlightId() + " " + fareClass + " " + status;
    }
}
