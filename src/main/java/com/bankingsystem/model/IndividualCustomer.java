package com.bankingsystem.model;

import java.time.LocalDate;

/**
 * Represents an individual retail banking customer.
 */
public class IndividualCustomer extends Customer {
    private final String nationalID;
    private final LocalDate dateOfBirth;
    private boolean personalAccountRequested;

    public IndividualCustomer(String customerID, String firstName, String surname,
                              String email, String phoneNumber, String address,
                              Authentication authentication, String nationalID, LocalDate dateOfBirth) {
        super(customerID, firstName, surname, email, phoneNumber, address, authentication);
        if (nationalID == null || nationalID.isBlank()) {
            throw new IllegalArgumentException("National ID must not be blank.");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth must not be null.");
        }
        this.nationalID = nationalID;
        this.dateOfBirth = dateOfBirth;
        this.personalAccountRequested = false;
    }

    public void openPersonalAccount() {
        this.personalAccountRequested = true;
    }

    public void updateIndividualDetails(String phone, String address) {
        updatePhoneNumber(phone);
        updateAddress(address);
    }

    public String getNationalID() {
        return nationalID;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public boolean isPersonalAccountRequested() {
        return personalAccountRequested;
    }

    @Override
    public String getCustomerType() {
        return "IndividualCustomer";
    }
}
