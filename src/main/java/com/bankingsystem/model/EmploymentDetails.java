package com.bankingsystem.model;

import com.bankingsystem.interfaces.Verifiable;

/**
 * Holds and verifies customer employment information required for ChequeAccount eligibility.
 */
public class EmploymentDetails implements Verifiable {
    private String companyName;
    private String companyAddress;
    private boolean isEmployed;
    private boolean verified;

    public EmploymentDetails(String companyName, String companyAddress, boolean isEmployed) {
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.isEmployed = isEmployed;
        this.verified = false;
    }

    @Override
    public boolean verifyDetails() {
        this.verified = isEmployed
                && companyName != null && !companyName.isBlank()
                && companyAddress != null && !companyAddress.isBlank();
        return this.verified;
    }

    @Override
    public boolean getVerificationStatus() {
        return verified;
    }

    public boolean isEmployed() {
        return isEmployed;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }
}
