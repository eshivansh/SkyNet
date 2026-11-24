package com.skynet.concurrency;

import com.skynet.model.Flight;
import com.skynet.service.BookingAndGateService;

import java.util.List;

public class FlightMonitoringDaemon extends Thread {
    private final BookingAndGateService service;
    private volatile int intervalMillis;
    private int reports = 0;

    public FlightMonitoringDaemon(BookingAndGateService service, int intervalMillis) {
        super("Flight-Monitor");
        this.service = service;
        this.intervalMillis = intervalMillis;
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                reports++;
                printReport();
                Thread.sleep(intervalMillis);
            }
        } catch (InterruptedException e) {
            System.out.println("  [" + getName() + "] interrupted, stopped after " + reports + " report(s).");
        }
    }

    private void printReport() {
        List<Flight> flights = service.getAllFlights();
        StringBuilder report = new StringBuilder();
        report.append("\n  [" + getName() + " | report " + reports + "]\n");
        for (Flight flight : flights) {
            int free = flight.getAvailableSeats();
            String note = "OK";
            if (free == 0) {
                note = "FULL";
            } else if (free * 5 <= flight.getTotalSeats()) {
                note = "almost full";
            }
            report.append(String.format("    %-6s %2d of %2d seats free  %s%n",
                    flight.getFlightId(), free, flight.getTotalSeats(), note));
        }
        System.out.print(report);
    }

    public void setIntervalMillis(int intervalMillis) {
        this.intervalMillis = intervalMillis;
    }
}
