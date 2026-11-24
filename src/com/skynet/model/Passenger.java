package com.skynet.model;

import com.skynet.enums.LoyaltyTier;

public class Passenger {
    private final String passengerId;
    private final String name;
    private final LoyaltyTier tier;
    private double walletBalance;

    public Passenger(String passengerId, String name, LoyaltyTier tier, double walletBalance) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Passenger name cannot be empty.");
        }
        if (walletBalance < 0) {
            throw new IllegalArgumentException("Wallet balance cannot be negative.");
        }
        this.passengerId = passengerId;
        this.name = name.trim();
        this.tier = tier;
        this.walletBalance = walletBalance;
    }

    public synchronized void debit(double amount) {
        walletBalance = walletBalance - amount;
    }

    public synchronized void credit(double amount) {
        walletBalance = walletBalance + amount;
    }

    public synchronized double getWalletBalance() {
        return walletBalance;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getName() {
        return name;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    @Override
    public String toString() {
        return passengerId + " " + name + " (" + tier + ")";
    }
}
