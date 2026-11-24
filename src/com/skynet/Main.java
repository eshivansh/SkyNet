package com.skynet;

import com.skynet.concurrency.ConcurrentBookingSimulator;
import com.skynet.concurrency.FlightMonitoringDaemon;
import com.skynet.concurrency.ThreadLifecycleDemo;
import com.skynet.enums.LoyaltyTier;
import com.skynet.model.Passenger;
import com.skynet.repository.InMemoryAirlineStore;
import com.skynet.service.BookingAndGateService;

import java.util.ArrayList;
import java.util.List;
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
        System.out.println("10. Concurrent booking simulation");
        System.out.println("11. Thread lifecycle demo");
        System.out.println("12. Flight monitor (daemon thread)");
        System.out.println(" 0. Exit");
    }

    private static boolean handleChoice(int choice) throws InterruptedException {
        switch (choice) {
            case 10: runSimulation(); break;
            case 11: ThreadLifecycleDemo.runDemo(); break;
            case 12: toggleMonitor(); break;
            case 0: return false;
            default: System.out.println("  Please choose a number from the menu.");
        }
        return true;
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
