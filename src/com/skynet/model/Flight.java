package com.skynet.model;

import com.skynet.enums.FareClass;

public class Flight {
    private final String flightId;
    private final String source;
    private final String destination;
    private final int totalSeats;
    private final double baseFare;
    private int availableSeats;

    public Flight(String flightId, String source, String destination, int totalSeats, double baseFare) {
        if (flightId == null || flightId.trim().isEmpty()) {
            throw new IllegalArgumentException("Flight ID cannot be empty.");
        }
        if (source.trim().equalsIgnoreCase(destination.trim())) {
            throw new IllegalArgumentException("Source and destination cannot be the same.");
        }
        if (totalSeats <= 0 || baseFare <= 0) {
            throw new IllegalArgumentException("Seats and base fare must be greater than zero.");
        }
        this.flightId = flightId.trim().toUpperCase();
        this.source = source.trim();
        this.destination = destination.trim();
        this.totalSeats = totalSeats;
        this.baseFare = baseFare;
        this.availableSeats = totalSeats;
    }

    public boolean reserveSeat() {
        synchronized (this) {
            if (availableSeats > 0) {
                availableSeats--;
                return true;
            }
            return false;
        }
    }

    public synchronized void releaseSeat() {
        if (availableSeats < totalSeats) {
            availableSeats++;
        }
    }

    public synchronized int getAvailableSeats() {
        return availableSeats;
    }

    public int getBookedSeats() {
        return totalSeats - getAvailableSeats();
    }

    public double getFare(FareClass fareClass) {
        double fare = baseFare * fareClass.getMultiplier();
        if (getAvailableSeats() * 5 <= totalSeats) {
            fare = fare * 1.2;
        }
        return fare;
    }

    public int getStandbyLimit() {
        return totalSeats;
    }

    public String getFlightId() {
        return flightId;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public double getBaseFare() {
        return baseFare;
    }

    @Override
    public String toString() {
        return flightId + " (" + source + " -> " + destination + ")";
    }
}
