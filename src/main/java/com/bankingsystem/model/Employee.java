package com.bankingsystem.model;

/**
 * Represents a bank employee assigned to a Branch and employed by a Bank.
 */
public class Employee {
    private String employeeID;
    private String name;
    private String position;
    private double salary;
    private boolean checkedIn;
    private String lastTaskPerformed;

    public Employee(String employeeID, String name, String position, double salary) {
        if (employeeID == null || employeeID.isBlank()) {
            throw new IllegalArgumentException("Employee ID must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Employee name must not be blank.");
        }
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative.");
        }
        this.employeeID = employeeID;
        this.name = name;
        this.position = position;
        this.salary = salary;
        this.checkedIn = false;
    }

    public void checkIn() {
        this.checkedIn = true;
    }

    public void checkOut() {
        this.checkedIn = false;
    }

    public void performTask(String task) {
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("Task description must not be blank.");
        }
        this.lastTaskPerformed = task;
    }

    public String getName() {
        return name;
    }

    public String getEmployeeID() {
        return employeeID;
    }

    public String getPosition() {
        return position;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative.");
        }
        this.salary = salary;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public String getLastTaskPerformed() {
        return lastTaskPerformed;
    }
}
