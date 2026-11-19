package com.bankingsystem.model;

/**
 * Represents an ATM operated by the bank.
 */
public class ATM {
    private final String atmId;
    private final String location;
    private double cashAvailable;

    public ATM(String atmId, String location, double cashAvailable) {
        if (atmId == null || atmId.isBlank()) {
            throw new IllegalArgumentException("ATM ID must not be blank.");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("ATM location must not be blank.");
        }
        if (cashAvailable < 0) {
            throw new IllegalArgumentException("ATM cash balance cannot be negative.");
        }
        this.atmId = atmId;
        this.location = location;
        this.cashAvailable = cashAvailable;
    }

    public boolean dispenseCash(double amount) {
        if (amount <= 0 || amount > cashAvailable) {
            return false;
        }
        cashAvailable -= amount;
        return true;
    }

    public String getAtmId() {
        return atmId;
    }

    public String getLocation() {
        return location;
    }

    public double getCashAvailable() {
        return cashAvailable;
    }
}
