package com.scc.dao;

import com.scc.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Customer table: read and update a customer's profile. */
public class CustomerDao {

    public static Customer find(int customerId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT customer_id, name, contact_number, address FROM Customer WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new Customer(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null;
            }
        }
    }

    public static void update(int customerId, String name, String contact, String address) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE Customer SET name = ?, contact_number = ?, address = ? WHERE customer_id = ?")) {
            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setString(3, address);
            ps.setInt(4, customerId);
            ps.executeUpdate();
        }
    }
}
