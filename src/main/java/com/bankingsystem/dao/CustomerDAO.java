package com.bankingsystem.dao;

import com.bankingsystem.model.Authentication;
import com.bankingsystem.model.CorporateCustomer;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.IndividualCustomer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Data Access Object for Customer and EmploymentDetails CRUD operations.
 */
public class CustomerDAO {

    public void create(Customer customer, String rawPassword) throws SQLException {
        String sql = """
                INSERT INTO customers (
                    customer_id, customer_type, first_name, surname, email, phone_number, address,
                    national_id, date_of_birth, company_name, company_address, registration_number,
                    username, password
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerID());
            ps.setString(2, customer.getCustomerType());
            ps.setString(3, customer.getFirstName());
            ps.setString(4, customer.getSurname());
            ps.setString(5, customer.getEmail());
            ps.setString(6, customer.getPhoneNumber());
            ps.setString(7, customer.getAddress());

            if (customer instanceof IndividualCustomer ind) {
                ps.setString(8, ind.getNationalID());
                ps.setString(9, ind.getDateOfBirth().toString());
                ps.setString(10, null);
                ps.setString(11, null);
                ps.setString(12, null);
            } else if (customer instanceof CorporateCustomer corp) {
                ps.setString(8, null);
                ps.setString(9, null);
                ps.setString(10, corp.getCompanyName());
                ps.setString(11, corp.getCompanyAddress());
                ps.setString(12, corp.getRegistrationNumber());
            }

            ps.setString(13, customer.getAuthentication().getUsername());
            ps.setString(14, rawPassword);
            ps.executeUpdate();
        }
    }

    public Optional<Customer> findById(String customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Customer> authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM customers WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Customer customer = mapCustomer(rs);
                    customer.getAuthentication().login();
                    return Optional.of(customer);
                }
            }
        }
        return Optional.empty();
    }

    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT * FROM customers ORDER BY customer_id";
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCustomer(rs));
            }
        }
        return list;
    }

    public int nextCustomerSequence() throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(CAST(SUBSTR(customer_id, 6) AS INTEGER)), 0) + 1
                FROM customers
                WHERE customer_id LIKE 'CUST-%'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    public boolean updateContactDetails(String customerId, String email, String phone, String address) throws SQLException {
        String sql = "UPDATE customers SET email = ?, phone_number = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, phone);
            ps.setString(3, address);
            ps.setString(4, customerId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(String customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            return ps.executeUpdate() > 0;
        }
    }

    public void saveEmploymentDetails(String customerId, EmploymentDetails details) throws SQLException {
        String sql = """
                INSERT INTO employment_details (customer_id, company_name, company_address, is_employed, verified)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(customer_id) DO UPDATE SET
                    company_name = excluded.company_name,
                    company_address = excluded.company_address,
                    is_employed = excluded.is_employed,
                    verified = excluded.verified
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, details.getCompanyName());
            ps.setString(3, details.getCompanyAddress());
            ps.setInt(4, details.isEmployed() ? 1 : 0);
            ps.setInt(5, details.verifyDetails() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public Optional<EmploymentDetails> findEmploymentDetails(String customerId) throws SQLException {
        String sql = "SELECT * FROM employment_details WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EmploymentDetails details = new EmploymentDetails(
                            rs.getString("company_name"),
                            rs.getString("company_address"),
                            rs.getInt("is_employed") == 1
                    );
                    details.verifyDetails();
                    return Optional.of(details);
                }
            }
        }
        return Optional.empty();
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        String id = rs.getString("customer_id");
        String type = rs.getString("customer_type");
        String firstName = rs.getString("first_name");
        String surname = rs.getString("surname");
        String email = rs.getString("email");
        String phone = rs.getString("phone_number");
        String address = rs.getString("address");
        Authentication auth = new Authentication(rs.getString("username"), rs.getString("password"));

        if ("CorporateCustomer".equals(type)) {
            return new CorporateCustomer(
                    id, firstName, surname, email, phone, address, auth,
                    rs.getString("company_name"),
                    rs.getString("company_address"),
                    rs.getString("registration_number")
            );
        }
        LocalDate dob = LocalDate.parse(rs.getString("date_of_birth"));
        return new IndividualCustomer(
                id, firstName, surname, email, phone, address, auth,
                rs.getString("national_id"), dob
        );
    }
}
