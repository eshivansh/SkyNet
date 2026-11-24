package com.skynet.exception;

public class InsufficientWalletBalanceException extends Exception {
    public InsufficientWalletBalanceException(String passengerName, double fare, double balance) {
        super(String.format("%s cannot pay Rs.%,.2f (wallet has Rs.%,.2f)", passengerName, fare, balance));
    }
}
