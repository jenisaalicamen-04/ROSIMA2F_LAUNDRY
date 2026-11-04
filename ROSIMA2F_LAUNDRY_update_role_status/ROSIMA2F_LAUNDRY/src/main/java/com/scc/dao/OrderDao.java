package com.scc.dao;

import com.scc.model.LaundryOrder;
import com.scc.model.StatusReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Constants.STATUSES;
import static com.scc.util.Constants.now;

public class OrderDao {
    private static final String BASE =
            "SELECT o.order_id, o.customer_id, c.name, o.processed_by, o.service_type, "
                    + "o.order_date, o.status, o.total_amount "
                    + "FROM LaundryOrder o JOIN Customer c ON c.customer_id = o.customer_id ";

    private static LaundryOrder map(ResultSet rs) throws SQLException {
        int pb = rs.getInt(4);
        Integer processedBy = rs.wasNull() ? null : pb;
        return new LaundryOrder(rs.getInt(1), rs.getInt(2), rs.getString(3), processedBy,
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getDouble(8));
    }

    private static List<LaundryOrder> query(String where, Object... params) throws SQLException {
        List<LaundryOrder> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + where + " ORDER BY o.order_id DESC")) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** CREATE: new order, always starts as Pending. Returns the new order_id. */
    public static int create(int customerId, String serviceType) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO LaundryOrder(customer_id, service_type, order_date, status, total_amount) "
                             + "VALUES(?,?,?,'Pending',0)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setString(2, serviceType);
            ps.setString(3, now());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    // ---- READ ----
    public static List<LaundryOrder> findAll() throws SQLException {
        return query("");
    }

    public static List<LaundryOrder> findByCustomer(int customerId) throws SQLException {
        return query("WHERE o.customer_id = ?", customerId);
    }

    public static List<LaundryOrder> findByStatus(String status) throws SQLException {
        return query("WHERE o.status = ?", status);
    }

    public static List<LaundryOrder> findByCustomerName(String name) throws SQLException {
        return query("WHERE c.name LIKE ?", "%" + name + "%");
    }

    public static LaundryOrder findById(int orderId) throws SQLException {
        List<LaundryOrder> list = query("WHERE o.order_id = ?", orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ---- UPDATE ----
    public static void updateInfo(int orderId, String serviceType, double totalAmount, int staffId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE LaundryOrder SET service_type = ?, total_amount = ?, processed_by = ? WHERE order_id = ?")) {
            ps.setString(1, serviceType);
            ps.setDouble(2, totalAmount);
            ps.setInt(3, staffId);
            ps.setInt(4, orderId);
            ps.executeUpdate();
        }
    }

    public static void updateStatus(int orderId, String status, int staffId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE LaundryOrder SET status = ?, processed_by = ? WHERE order_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, staffId);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        }
    }

    // ---- DELETE (soft): the row stays for history, status becomes Cancelled ----
    public static void cancel(int orderId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE LaundryOrder SET status = 'Cancelled' WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    // ---- REPORT ----
    public static StatusReport report() throws SQLException {
        int[] n = new int[STATUSES.length];
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT status, COUNT(*) FROM LaundryOrder GROUP BY status")) {
            while (rs.next()) {
                for (int i = 0; i < STATUSES.length; i++) {
                    if (STATUSES[i].equals(rs.getString(1))) n[i] = rs.getInt(2);
                }
            }
        }
        int total = 0;
        for (int v : n) total += v;
        return new StatusReport(total, n[0], n[1], n[2], n[3], n[4]);
    }
}
