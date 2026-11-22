package com.bankingsystem.dao;

import com.bankingsystem.model.Authentication;
import com.bankingsystem.model.CorporateCustomer;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.IndividualCustomer;
import com.bankingsystem.security.CustomerDataCipher;
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
    private final CustomerDataCipher dataCipher;

    public CustomerDAO() {
        this(CustomerDataCipher.fromEnvironment());
    }

    public CustomerDAO(CustomerDataCipher dataCipher) {
        if (dataCipher == null) {
            throw new IllegalArgumentException("Customer data cipher cannot be null.");
        }
        this.dataCipher = dataCipher;
    }

    public void create(Customer customer) throws SQLException {
        String sql = """
                INSERT INTO customers (
                    customer_id, customer_type, first_name, surname, email, phone_number, address,
                    national_id, date_of_birth, company_name, company_address, registration_number,
                    username, password_hash, email_lookup
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerID());
            ps.setString(2, customer.getCustomerType());
            ps.setString(3, dataCipher.encrypt(customer.getFirstName()));
            ps.setString(4, dataCipher.encrypt(customer.getSurname()));
            ps.setString(5, dataCipher.encrypt(customer.getEmail()));
            ps.setString(6, dataCipher.encrypt(customer.getPhoneNumber()));
            ps.setString(7, dataCipher.encrypt(customer.getAddress()));

            if (customer instanceof IndividualCustomer ind) {
                ps.setString(8, dataCipher.encrypt(ind.getNationalID()));
                ps.setString(9, dataCipher.encrypt(ind.getDateOfBirth().toString()));
                ps.setString(10, null);
                ps.setString(11, null);
                ps.setString(12, null);
            } else if (customer instanceof CorporateCustomer corp) {
                ps.setString(8, null);
                ps.setString(9, null);
                ps.setString(10, dataCipher.encrypt(corp.getCompanyName()));
                ps.setString(11, dataCipher.encrypt(corp.getCompanyAddress()));
                ps.setString(12, dataCipher.encrypt(corp.getRegistrationNumber()));
            } else {
                throw new IllegalArgumentException("Unsupported customer type: " + customer.getCustomerType());
            }

            ps.setString(13, customer.getAuthentication().getUsername());
            ps.setString(14, customer.getAuthentication().getPasswordHash());
            ps.setString(15, dataCipher.emailLookup(customer.getEmail()));
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
        String sql = "SELECT * FROM customers WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Authentication authentication = Authentication.fromPasswordHash(
                            rs.getString("username"), rs.getString("password_hash"));
                    if (authentication.login(username, password)) {
                        return Optional.of(mapCustomer(rs, authentication));
                    }
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
        String sql = "UPDATE customers SET email = ?, email_lookup = ?, phone_number = ?, address = ? "
                + "WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataCipher.encrypt(email));
            ps.setString(2, dataCipher.emailLookup(email));
            ps.setString(3, dataCipher.encrypt(phone));
            ps.setString(4, dataCipher.encrypt(address));
            ps.setString(5, customerId);
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
            ps.setString(2, dataCipher.encrypt(details.getCompanyName()));
            ps.setString(3, dataCipher.encrypt(details.getCompanyAddress()));
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
                            dataCipher.decrypt(rs.getString("company_name")),
                            dataCipher.decrypt(rs.getString("company_address")),
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
        Authentication auth = Authentication.fromPasswordHash(
                rs.getString("username"), rs.getString("password_hash"));
        return mapCustomer(rs, auth);
    }

    private Customer mapCustomer(ResultSet rs, Authentication auth) throws SQLException {
        String id = rs.getString("customer_id");
        String type = rs.getString("customer_type");

        if ("CorporateCustomer".equals(type)) {
            return new CorporateCustomer(
                    id, dataCipher.decrypt(rs.getString("first_name")),
                    dataCipher.decrypt(rs.getString("surname")),
                    dataCipher.decrypt(rs.getString("email")),
                    dataCipher.decrypt(rs.getString("phone_number")),
                    dataCipher.decrypt(rs.getString("address")), auth,
                    dataCipher.decrypt(rs.getString("company_name")),
                    dataCipher.decrypt(rs.getString("company_address")),
                    dataCipher.decrypt(rs.getString("registration_number"))
            );
        }
        String dateOfBirth = dataCipher.decrypt(rs.getString("date_of_birth"));
        LocalDate dob = LocalDate.parse(dateOfBirth);
        return new IndividualCustomer(
                id, dataCipher.decrypt(rs.getString("first_name")),
                dataCipher.decrypt(rs.getString("surname")),
                dataCipher.decrypt(rs.getString("email")),
                dataCipher.decrypt(rs.getString("phone_number")),
                dataCipher.decrypt(rs.getString("address")), auth,
                dataCipher.decrypt(rs.getString("national_id")), dob
        );
    }
}
