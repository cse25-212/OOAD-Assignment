package com.bankingsystem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class representing a bank Customer (Individual or Corporate).
 * Enforces encapsulation and polymorphic ownership of multiple Account instances.
 */
public abstract class Customer {
    private final String customerID;
    private String firstName;
    private String surname;
    private String email;
    private String phoneNumber;
    private String address;
    private Authentication authentication;
    private final List<Account> accounts;

    protected Customer(String customerID, String firstName, String surname,
                       String email, String phoneNumber, String address,
                       Authentication authentication) {
        if (customerID == null || customerID.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        this.customerID = customerID;
        this.firstName = firstName;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.authentication = authentication;
        this.accounts = new ArrayList<>();
    }

    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }
        if (!this.customerID.equals(account.getCustomerId())) {
            throw new IllegalArgumentException("Account customerId does not match this customer.");
        }
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    public void updatePhoneNumber(String newPhone) {
        if (newPhone == null || newPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be blank.");
        }
        this.phoneNumber = newPhone;
    }

    public void updateEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank.");
        }
        this.email = newEmail;
    }

    public void updateAddress(String newAddress) {
        if (newAddress == null || newAddress.isBlank()) {
            throw new IllegalArgumentException("Address must not be blank.");
        }
        this.address = newAddress;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public String getCustomerID() {
        return customerID;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public abstract String getCustomerType();
}
