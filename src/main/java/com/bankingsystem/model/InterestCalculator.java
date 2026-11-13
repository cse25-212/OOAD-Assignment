package com.bankingsystem.model;

import com.bankingsystem.interfaces.InterestPayable;
import java.time.LocalDate;
import java.util.List;

/**
 * Utility domain service that calculates and batch-applies monthly interest
 * across polymorphic InterestPayable accounts.
 */
public class InterestCalculator {
    private LocalDate calculationDate;

    public InterestCalculator() {
        this.calculationDate = LocalDate.now();
    }

    public InterestCalculator(LocalDate calculationDate) {
        this.calculationDate = calculationDate != null ? calculationDate : LocalDate.now();
    }

    public double computeForAccount(InterestPayable account) {
        if (account == null) {
            throw new IllegalArgumentException("InterestPayable account cannot be null.");
        }
        return account.calculateMonthlyInterest();
    }

    public int batchProcessMonthlyYield(List<InterestPayable> accounts) {
        if (accounts == null) {
            return 0;
        }
        int processedCount = 0;
        this.calculationDate = LocalDate.now();
        for (InterestPayable account : accounts) {
            if (account != null) {
                account.applyInterest();
                processedCount++;
            }
        }
        return processedCount;
    }

    public LocalDate getCalculationDate() {
        return calculationDate;
    }
}
