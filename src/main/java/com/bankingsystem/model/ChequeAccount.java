package com.bankingsystem.model;

import com.bankingsystem.interfaces.Withdrawable;

/**
 * ChequeAccount used for salary credits and unlimited deposits/withdrawals up to balance.
 * Requires verified EmploymentDetails before opening.
 */
public class ChequeAccount extends Account implements Withdrawable {
    private final String employerName;
    private final String employerAddress;
    private final EmploymentDetails employmentDetails;

    public ChequeAccount(String accountNumber, double initialBalance, String branchCode,
                         String customerId, EmploymentDetails employmentDetails) {
        super(accountNumber, initialBalance, branchCode, customerId);
        if (employmentDetails == null || !employmentDetails.verifyDetails()) {
            throw new IllegalArgumentException(
                    "ChequeAccount can only be opened for an employed customer with verified employment details."
            );
        }
        this.employmentDetails = employmentDetails;
        this.employerName = employmentDetails.getCompanyName();
        this.employerAddress = employmentDetails.getCompanyAddress();
    }

    @Override
    public boolean withdraw(double amount) {
        if (debit(amount)) {
            getTransactionHistory().logAudit(
                    String.format("Withdrew BWP %.2f from ChequeAccount %s. Remaining balance: BWP %.2f",
                            amount, getAccountNumber(), getBalance())
            );
            return true;
        }
        getTransactionHistory().logAudit(
                String.format("Failed withdrawal attempt of BWP %.2f on ChequeAccount %s.", amount, getAccountNumber())
        );
        return false;
    }

    public void depositSalary(double amount) {
        if (!validateEmployment()) {
            throw new IllegalStateException("Cannot credit salary: employment details are not verified.");
        }
        deposit(amount, "Salary Credit - " + employerName);
    }

    public boolean validateEmployment() {
        return employmentDetails != null && employmentDetails.isEmployed() && employmentDetails.verifyDetails();
    }

    public String getEmployerName() {
        return employerName;
    }

    public String getEmployerAddress() {
        return employerAddress;
    }

    public EmploymentDetails getEmploymentDetails() {
        return employmentDetails;
    }

    @Override
    public String getAccountType() {
        return "ChequeAccount";
    }
}
