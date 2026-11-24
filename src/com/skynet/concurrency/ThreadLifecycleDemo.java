package com.skynet.concurrency;

public class ThreadLifecycleDemo implements Runnable {
    private static final Object COUNTER_LOCK = new Object();

    private static volatile boolean keepWorking = true;

    private final String role;
    private final Thread threadToJoin;

    public ThreadLifecycleDemo(String role, Thread threadToJoin) {
        this.role = role;
        this.threadToJoin = threadToJoin;
    }

    @Override
    public void run() {
        try {
            if (role.equals("SCANNER")) {
                long checks = 0;
                while (keepWorking) {
                    checks++;
                }
                System.out.println("    [Seat-Scanner] finished after " + checks + " checks.");
            } else if (role.equals("AGENT")) {
                synchronized (COUNTER_LOCK) {
                    Thread.sleep(1500);
                }
            } else if (role.equals("SUPERVISOR")) {
                threadToJoin.join();
            } else if (role.equals("NOTIFIER")) {
                Thread.sleep(10000);
                System.out.println("    [Notifier] woke up normally.");
            }
        } catch (InterruptedException e) {
            System.out.println("    [" + Thread.currentThread().getName() + "] got InterruptedException, stopping early.");
        }
    }

    public static void runDemo() throws InterruptedException {
        System.out.println("\n===== THREAD LIFECYCLE DEMO (main thread: " + Thread.currentThread().getName() + ") =====");

        keepWorking = true;
        Thread scanner = new Thread(new ThreadLifecycleDemo("SCANNER", null), "Seat-Scanner");
        scanner.setPriority(Thread.MAX_PRIORITY);
        show(scanner, "created");
        scanner.start();
        show(scanner, "after start()", waitFor(scanner, Thread.State.RUNNABLE));
        Thread.sleep(50);
        keepWorking = false;
        scanner.join();
        show(scanner, "after join()");

        Thread agentA = new Thread(new ThreadLifecycleDemo("AGENT", null), "Gate-Agent-A");
        Thread agentB = new Thread(new ThreadLifecycleDemo("AGENT", null), "Gate-Agent-B");
        Thread supervisor = new Thread(new ThreadLifecycleDemo("SUPERVISOR", agentA), "Supervisor");
        agentA.start();
        show(agentA, "sleeping inside the lock", waitFor(agentA, Thread.State.TIMED_WAITING));
        agentB.start();
        show(agentB, "wants the same lock", waitFor(agentB, Thread.State.BLOCKED));
        supervisor.start();
        show(supervisor, "inside agentA.join()", waitFor(supervisor, Thread.State.WAITING));
        agentA.join();
        agentB.join();
        supervisor.join();
        show(agentB, "after finishing");

        Thread notifier = new Thread(new ThreadLifecycleDemo("NOTIFIER", null), "Notifier");
        notifier.start();
        show(notifier, "sleeping 10 s", waitFor(notifier, Thread.State.TIMED_WAITING));
        notifier.interrupt();
        notifier.join(2000);
        show(notifier, "after interrupt()");

        System.out.println("  Priorities: MIN=" + Thread.MIN_PRIORITY + ", NORM=" + Thread.NORM_PRIORITY
                + ", MAX=" + Thread.MAX_PRIORITY + " (Seat-Scanner used MAX). Priority is only a hint.");
    }

    private static Thread.State waitFor(Thread thread, Thread.State expected) throws InterruptedException {
        for (int i = 0; i < 200 && thread.getState() != expected; i++) {
            Thread.sleep(5);
        }
        return thread.getState();
    }

    private static void show(Thread thread, String moment) {
        show(thread, moment, thread.getState());
    }

    private static void show(Thread thread, String moment, Thread.State state) {
        System.out.printf("  %-14s %-26s state=%-13s alive=%s%n", thread.getName(), moment, state, thread.isAlive());
    }
}
