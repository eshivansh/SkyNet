package com.skynet.enums;

public enum FareClass {
    ECONOMY(3, 1.0),
    BUSINESS(2, 2.0),
    FIRST(1, 3.0);

    private final int rank;
    private final double multiplier;

    FareClass(int rank, double multiplier) {
        this.rank = rank;
        this.multiplier = multiplier;
    }

    public int getRank() {
        return rank;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
