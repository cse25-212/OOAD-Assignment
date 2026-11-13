package com.bankingsystem.model;

/**
 * Represents a corporate or organizational banking customer.
 */
public class CorporateCustomer extends Customer {
    private String companyName;
    private String companyAddress;
    private final String registrationNumber;
    private boolean businessAccountRequested;

    public CorporateCustomer(String customerID, String representativeFirstName, String representativeSurname,
                             String email, String phoneNumber, String address,
                             Authentication authentication, String companyName,
                             String companyAddress, String registrationNumber) {
        super(customerID, representativeFirstName, representativeSurname, email, phoneNumber, address, authentication);
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank.");
        }
        if (registrationNumber == null || registrationNumber.isBlank()) {
            throw new IllegalArgumentException("Registration number must not be blank.");
        }
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.registrationNumber = registrationNumber;
        this.businessAccountRequested = false;
    }

    public void openBusinessAccount() {
        this.businessAccountRequested = true;
    }

    public void updateCompanyDetails(String name, String address) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Company address must not be blank.");
        }
        this.companyName = name;
        this.companyAddress = address;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public boolean isBusinessAccountRequested() {
        return businessAccountRequested;
    }

    @Override
    public String getCustomerType() {
        return "CorporateCustomer";
    }
}
