package com.bankingsystem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a physical branch operated by a Bank.
 */
public class Branch {
    private String branchCode;
    private String address;
    private boolean isOpen;
    private final List<Employee> employees;

    public Branch(String branchCode, String address) {
        if (branchCode == null || branchCode.isBlank()) {
            throw new IllegalArgumentException("Branch code must not be blank.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Branch address must not be blank.");
        }
        this.branchCode = branchCode;
        this.address = address;
        this.isOpen = true;
        this.employees = new ArrayList<>();
    }

    public void open() {
        this.isOpen = true;
    }

    public void close() {
        this.isOpen = false;
    }

    public void assignEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null.");
        }
        if (!employees.contains(employee)) {
            employees.add(employee);
        }
    }

    public List<Employee> getEmployees() {
        return Collections.unmodifiableList(employees);
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getAddress() {
        return address;
    }

    public boolean isOpen() {
        return isOpen;
    }
}
