# SkyNet

Airline seat booking system with overbooking and a standby list. Made for the Programming in Java (25CSE0202) project, Evaluation 1.

## About the project

Airlines sell a few more tickets than the number of seats because some passengers never turn up. If everyone does turn up, someone has to be moved to standby, and when a ticket is cancelled the free seat has to go to the right person.

In SkyNet many passengers can book the same flight at the same time without two people getting the same seat. When a flight is full, passengers go on a standby list. The list is ordered by loyalty tier, then fare class, then booking time. Cancelling a ticket refunds 90% of the fare and gives the seat to the first person on the standby list who can pay for it.

## Features

- Add flights and register passengers (some sample data is loaded at the start)
- Book a ticket: confirmed, standby if the flight is full, or rejected if standby is full too
- Last 20% of the seats cost 20% more
- Cancel a ticket with a 90% refund and standby promotion
- Standby list and gate boarding order
- Booking simulation with 10 to 20 threads, first without synchronization and then with it
- Thread lifecycle demo showing all six thread states
- Flight monitor running as a daemon thread

## How to run

You need JDK 11 or newer.

```
javac -d out $(find src -name "*.java")
java -cp out com.skynet.Main
```

On Windows PowerShell use this to compile:

```
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

## Menu

```
 1. View flights          7. View all bookings
 2. Add flight            8. View standby list
 3. Register passenger    9. Gate boarding order
 4. View passengers      10. Concurrent booking simulation
 5. Book ticket          11. Thread lifecycle demo
 6. Cancel ticket        12. Flight monitor (daemon thread)
 0. Exit
```

Sample data has 4 flights (SN101 to SN404) and 15 passengers (P101 to P115). Ticket IDs start from TKT-1001.

## Folder structure

```
src/com/skynet
    enums         LoyaltyTier, FareClass, TicketStatus
    model         Passenger, Flight, Ticket
    exception     FlightOverbookedException, InsufficientWalletBalanceException
    repository    InMemoryAirlineStore
    service       TicketPriorityComparator, BookingAndGateService
    concurrency   ConcurrentBookingSimulator, ThreadLifecycleDemo, FlightMonitoringDaemon
    Main.java
```

| Class | What it does |
|---|---|
| LoyaltyTier | VIP, PLATINUM, GOLD, SILVER with a rank |
| FareClass | ECONOMY, BUSINESS, FIRST with a rank and price multiplier |
| TicketStatus | CONFIRMED, STANDBY, CANCELLED |
| Passenger | passenger details and wallet |
| Flight | route, seats and seat booking |
| Ticket | one booking, sorted by booking time |
| FlightOverbookedException | thrown when no seat and no standby place is left |
| InsufficientWalletBalanceException | thrown when the wallet does not have enough money |
| InMemoryAirlineStore | keeps flights, passengers and tickets |
| TicketPriorityComparator | decides the order of the standby list |
| BookingAndGateService | booking, cancellation, refunds, standby and boarding |
| ConcurrentBookingSimulator | many threads booking the same flight |
| ThreadLifecycleDemo | thread states, sleep, join and interrupt |
| FlightMonitoringDaemon | background thread that shows free seats |
| Main | menu and sample data |

## Java topics used

| Topic | Where |
|---|---|
| Classes, constructors, encapsulation | model classes |
| Inheritance and interfaces | custom exceptions, FlightMonitoringDaemon extends Thread, Runnable, Comparable, Comparator |
| Enums | enums package |
| Strings, StringBuilder, wrapper classes | Main, FlightMonitoringDaemon |
| Custom exceptions, try / catch / finally, multi-catch | exception package, BookingAndGateService, Main |
| ArrayList, HashMap, LinkedHashMap, TreeMap | InMemoryAirlineStore |
| Iterator and ListIterator | InMemoryAirlineStore |
| Queue, Deque, PriorityQueue | BookingAndGateService |
| Comparable, Comparator, Collections.sort | Ticket, TicketPriorityComparator, BookingAndGateService |
| Thread, Runnable, start(), run() | ConcurrentBookingSimulator, FlightMonitoringDaemon |
| sleep(), join(), interrupt() | ThreadLifecycleDemo, FlightMonitoringDaemon |
| Thread states, daemon threads, priorities | ThreadLifecycleDemo, FlightMonitoringDaemon |
| Race condition | ConcurrentBookingSimulator |
| synchronized block and critical section | Flight |
| synchronized methods and intrinsic locks | Passenger, Flight, BookingAndGateService |
| volatile | ThreadLifecycleDemo, FlightMonitoringDaemon |

## Next evaluation

wait() and notify(), producer consumer, thread pools, JDBC with MySQL, lambdas, streams, generic classes and file handling.

## Team

| Name | Branch | Work |
|---|---|---|
| Shivansh | skynet-shiva | project setup, TicketPriorityComparator, BookingAndGateService, Main (booking options), README |
| Aashna | skynet-aashna | Flight, Ticket, InMemoryAirlineStore, ThreadLifecycleDemo |
| Aditya Sharma | skynet-as | enums, exceptions, Passenger |
| Aditya Gupta | skynet-ag | FlightMonitoringDaemon, ConcurrentBookingSimulator, Main (menu and thread options) |
