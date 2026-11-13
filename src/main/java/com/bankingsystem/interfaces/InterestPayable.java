package com.bankingsystem.interfaces;

/**
 * Contract for interest-bearing accounts that compute and apply monthly interest.
 */
public interface InterestPayable {
    double calculateMonthlyInterest();
    double applyInterest();
}
