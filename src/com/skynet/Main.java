package com.skynet;

import com.skynet.concurrency.ConcurrentBookingSimulator;
import com.skynet.concurrency.FlightMonitoringDaemon;
import com.skynet.concurrency.ThreadLifecycleDemo;
import com.skynet.enums.FareClass;
import com.skynet.enums.LoyaltyTier;
import com.skynet.exception.FlightOverbookedException;
import com.skynet.exception.InsufficientWalletBalanceException;
import com.skynet.model.Flight;
import com.skynet.model.Passenger;
import com.skynet.model.Ticket;
import com.skynet.repository.InMemoryAirlineStore;
import com.skynet.service.BookingAndGateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class Main {
    private static final BookingAndGateService service = new BookingAndGateService(new InMemoryAirlineStore());
    private static final Scanner scanner = new Scanner(System.in);
    private static FlightMonitoringDaemon monitor = null;

    public static void main(String[] args) {
        loadSampleData();
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            try {
                running = handleChoice(choice);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  ERROR: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("  Interrupted: " + e.getMessage());
            }
        }
        System.out.println("Thank you for flying SkyNet. Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n==========================================");
        System.out.println("      SKYNET AIRLINES - RESERVATIONS");
        System.out.println("==========================================");
        System.out.println(" 1. View flights          7. View all bookings");
        System.out.println(" 2. Add flight            8. View standby list");
        System.out.println(" 3. Register passenger    9. Gate boarding order");
        System.out.println(" 4. View passengers      10. Concurrent booking simulation");
        System.out.println(" 5. Book ticket          11. Thread lifecycle demo");
        System.out.println(" 6. Cancel ticket        12. Flight monitor (daemon thread)");
        System.out.println(" 0. Exit");
    }

    private static boolean handleChoice(int choice) throws InterruptedException {
        switch (choice) {
            case 1: viewFlights(); break;
            case 2: addFlight(); break;
            case 3: registerPassenger(); break;
            case 4: viewPassengers(service.getAllPassengers()); break;
            case 5: bookTicket(); break;
            case 6: cancelTicket(); break;
            case 7: viewBookings(); break;
            case 8: viewStandbyList(); break;
            case 9: viewBoardingOrder(); break;
            case 10: runSimulation(); break;
            case 11: ThreadLifecycleDemo.runDemo(); break;
            case 12: toggleMonitor(); break;
            case 0: return false;
            default: System.out.println("  Please choose a number from the menu.");
        }
        return true;
    }

    private static void viewFlights() {
        System.out.println("\n  Flight  Route                    Seats  Free  Standby  Economy fare");
        for (Flight f : service.getAllFlights()) {
            System.out.printf("  %-7s %-24s %5d %5d %8d  Rs.%,.2f%n", f.getFlightId(),
                    f.getSource() + " -> " + f.getDestination(), f.getTotalSeats(), f.getAvailableSeats(),
                    service.getStandbyCount(f.getFlightId()), f.getFare(FareClass.ECONOMY));
        }
    }

    private static void addFlight() {
        String id = readText("Flight ID (e.g. SN505): ");
        String from = readText("From: ");
        String to = readText("To: ");
        int seats = readInt("Number of seats: ");
        double fare = readDouble("Base fare (Rs.): ");
        Flight flight = service.addFlight(id, from, to, seats, fare);
        System.out.println("  Added " + flight);
    }

    private static void registerPassenger() {
        String name = readText("Name: ");
        LoyaltyTier tier = LoyaltyTier.values()[readOption("Tier", LoyaltyTier.values().length,
                "1=VIP 2=PLATINUM 3=GOLD 4=SILVER")];
        double wallet = readDouble("Wallet balance (Rs.): ");
        Passenger p = service.registerPassenger(name, tier, wallet);
        System.out.println("  Registered " + p);
        System.out.println("  Latest registrations (ListIterator, backwards):");
        viewPassengers(service.getRecentPassengers(3));
    }

    private static void viewPassengers(List<Passenger> passengers) {
        System.out.println("\n  ID     Name                Tier       Wallet");
        for (Passenger p : passengers) {
            System.out.printf("  %-6s %-19s %-9s Rs.%,.2f%n", p.getPassengerId(), p.getName(), p.getTier(),
                    p.getWalletBalance());
        }
    }

    private static void bookTicket() {
        String passengerId = readText("Passenger ID (e.g. P101): ");
        String flightId = readText("Flight ID (e.g. SN101): ");
        FareClass fareClass = FareClass.values()[readOption("Class", FareClass.values().length,
                "1=ECONOMY 2=BUSINESS 3=FIRST")];
        try {
            Ticket t = service.bookTicket(passengerId, flightId, fareClass);
            System.out.printf("  %s is %s, fare Rs.%,.2f%n", t.getTicketId(), t.getStatus(), t.getFare());
        } catch (FlightOverbookedException | InsufficientWalletBalanceException e) {
            System.out.println("  BOOKING FAILED: " + e.getMessage());
        } finally {
            System.out.println("  (booking request finished)");
        }
    }

    private static void cancelTicket() {
        String ticketId = readText("Ticket ID (e.g. TKT-1001): ");
        System.out.println("  " + service.cancelTicket(ticketId));
    }

    private static void viewBookings() {
        printTickets(service.getAllTickets());
        System.out.println("\n  Recent activity (newest first):");
        for (String line : service.getRecentActivity()) {
            System.out.println("   - " + line);
        }
    }

    private static void viewStandbyList() {
        String flightId = readText("Flight ID: ");
        List<Ticket> list = service.getStandbyList(flightId);
        if (list.isEmpty()) {
            System.out.println("  Nobody is on standby.");
            return;
        }
        System.out.println("  Standby list, best first (tier, then class, then booking time):");
        int rank = 1;
        for (Ticket t : list) {
            System.out.printf("  %d. %s %-18s %-9s %-9s booked %s%n", rank, t.getTicketId(), t.getPassenger().getName(),
                    t.getPassenger().getTier(), t.getFareClass(), t.getBookedAt().withNano(0));
            rank++;
        }
    }

    private static void viewBoardingOrder() {
        String flightId = readText("Flight ID: ");
        Queue<Ticket> queue = service.getBoardingQueue(flightId);
        if (queue.isEmpty()) {
            System.out.println("  No confirmed passengers yet.");
            return;
        }
        int position = 1;
        while (!queue.isEmpty()) {
            Ticket t = queue.poll();
            System.out.printf("  %d. %-18s %-9s %s%n", position, t.getPassenger().getName(),
                    t.getPassenger().getTier(), t.getFareClass());
            position++;
        }
    }

    private static void printTickets(List<Ticket> tickets) {
        System.out.println("\n  Ticket    Passenger          Flight  Class     Fare           Status");
        for (Ticket t : tickets) {
            System.out.printf("  %-9s %-18s %-7s %-9s Rs.%,-11.2f %s%n", t.getTicketId(), t.getPassenger().getName(),
                    t.getFlight().getFlightId(), t.getFareClass(), t.getFare(), t.getStatus());
        }
    }

    private static void runSimulation() throws InterruptedException {
        List<Passenger> all = service.getAllPassengers();
        if (all.size() < 10) {
            System.out.println("  Register at least 10 passengers first.");
            return;
        }
        int max = Math.min(20, all.size());
        int count = readInt("How many passengers book at once (10-" + max + "): ");
        if (count < 10 || count > max) {
            System.out.println("  Using " + max + ".");
            count = max;
        }
        List<Passenger> chosen = new ArrayList<>(all.subList(0, count));
        ConcurrentBookingSimulator.runSimulation(service, chosen);
    }

    private static void toggleMonitor() throws InterruptedException {
        if (monitor != null && monitor.isAlive()) {
            monitor.interrupt();
            monitor.join();
            return;
        }
        monitor = new FlightMonitoringDaemon(service, 1000);
        System.out.println("  Starting " + monitor.getName() + " | daemon=" + monitor.isDaemon()
                + " | priority=" + monitor.getPriority());
        monitor.start();
        Thread.sleep(3500);
        if (readText("Keep it running in the background? (y/n): ").equalsIgnoreCase("y")) {
            monitor.setIntervalMillis(30000);
            System.out.println("  It will report every 30 s. Choose 12 again to stop it.");
        } else {
            monitor.interrupt();
            monitor.join();
        }
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        String text = scanner.nextLine().trim();
        while (text.isEmpty()) {
            System.out.print("  Cannot be empty. " + prompt);
            text = scanner.nextLine().trim();
        }
        return text;
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readText(prompt));
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(readText(prompt));
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a number.");
            }
        }
    }

    private static int readOption(String label, int count, String options) {
        while (true) {
            int value = readInt(label + " (" + options + "): ");
            if (value >= 1 && value <= count) {
                return value - 1;
            }
            System.out.println("  Choose 1 to " + count + ".");
        }
    }

    private static void loadSampleData() {
        service.addFlight("SN101", "Delhi", "Mumbai", 5, 4500);
        service.addFlight("SN202", "Mumbai", "Bengaluru", 8, 3800);
        service.addFlight("SN303", "Bengaluru", "Kolkata", 6, 5200);
        service.addFlight("SN404", "Chennai", "Delhi", 10, 6000);

        service.registerPassenger("Aarav Sharma", LoyaltyTier.SILVER, 20000);
        service.registerPassenger("Priya Verma", LoyaltyTier.VIP, 50000);
        service.registerPassenger("Rohan Mehta", LoyaltyTier.GOLD, 15000);
        service.registerPassenger("Ananya Iyer", LoyaltyTier.PLATINUM, 60000);
        service.registerPassenger("Vikram Singh", LoyaltyTier.SILVER, 25000);
        service.registerPassenger("Sneha Kapoor", LoyaltyTier.GOLD, 40000);
        service.registerPassenger("Arjun Nair", LoyaltyTier.VIP, 18000);
        service.registerPassenger("Kavya Reddy", LoyaltyTier.PLATINUM, 12000);
        service.registerPassenger("Rahul Gupta", LoyaltyTier.SILVER, 2000);
        service.registerPassenger("Meera Joshi", LoyaltyTier.GOLD, 30000);
        service.registerPassenger("Karan Malhotra", LoyaltyTier.PLATINUM, 35000);
        service.registerPassenger("Isha Patel", LoyaltyTier.VIP, 80000);
        service.registerPassenger("Dev Bansal", LoyaltyTier.SILVER, 14000);
        service.registerPassenger("Neha Chawla", LoyaltyTier.GOLD, 9000);
        service.registerPassenger("Siddharth Rao", LoyaltyTier.SILVER, 7000);
        System.out.println("Sample data loaded: 4 flights, 15 passengers.");
    }
}
