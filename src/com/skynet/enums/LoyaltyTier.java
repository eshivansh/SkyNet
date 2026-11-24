package com.skynet.enums;

public enum LoyaltyTier {
    VIP(1),
    PLATINUM(2),
    GOLD(3),
    SILVER(4);

    private final int rank;

    LoyaltyTier(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
