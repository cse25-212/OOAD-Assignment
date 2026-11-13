package com.bankingsystem.model;

import com.bankingsystem.interfaces.Reportable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top-level Bank entity operating Branches, deploying ATMs, employing Staff, and serving Customers.
 */
public class Bank implements Reportable {
    private String name;
    private String address;
    private String sortCode;
    private final List<Branch> branches;
    private final List<ATM> atms;
    private final List<Employee> staff;
    private final List<Customer> customers;
    private boolean statementsExported;

    public Bank(String name, String address, String sortCode) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Bank name must not be blank.");
        }
        if (sortCode == null || sortCode.isBlank()) {
            throw new IllegalArgumentException("Sort code must not be blank.");
        }
        this.name = name;
        this.address = address;
        this.sortCode = sortCode;
        this.branches = new ArrayList<>();
        this.atms = new ArrayList<>();
        this.staff = new ArrayList<>();
        this.customers = new ArrayList<>();
        this.statementsExported = false;
    }

    public void addBranch(Branch branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Branch cannot be null.");
        }
        if (!branches.contains(branch)) {
            branches.add(branch);
        }
    }

    public void removeBranch(Branch branch) {
        branches.remove(branch);
    }

    public void addATM(ATM atm) {
        if (atm == null) {
            throw new IllegalArgumentException("ATM cannot be null.");
        }
        if (!atms.contains(atm)) {
            atms.add(atm);
        }
    }

    public void addStaff(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null.");
        }
        if (!staff.contains(employee)) {
            staff.add(employee);
        }
    }

    public void registerCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }
        if (!customers.contains(customer)) {
            customers.add(customer);
        }
    }

    public List<Branch> getBranches() {
        return Collections.unmodifiableList(branches);
    }

    public List<ATM> getAtms() {
        return Collections.unmodifiableList(atms);
    }

    public List<Employee> getStaff() {
        return Collections.unmodifiableList(staff);
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getSortCode() {
        return sortCode;
    }

    public boolean isStatementsExported() {
        return statementsExported;
    }

    @Override
    public String generateReport() {
        double totalDeposits = customers.stream()
                .flatMap(c -> c.getAccounts().stream())
                .mapToDouble(Account::getBalance)
                .sum();
        return String.format(
                "Bank Report: %s (Sort Code: %s) | Branches: %d | ATMs: %d | Staff: %d | Customers: %d | Total Deposits: BWP %.2f",
                name, sortCode, branches.size(), atms.size(), staff.size(), customers.size(), totalDeposits
        );
    }

    @Override
    public void exportStatements() {
        this.statementsExported = true;
    }
}
