package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Constants.now;

/**
 * SQL for the activity_logs table. Logs can only be added or read (no edit, no delete).
 *
 * Action types used in the system:
 *   USER_LOGIN, USER_LOGOUT, USER_REGISTERED, USER_CREATED, USER_APPROVED, USER_UPDATED, PROFILE_UPDATED,
 *   ORDER_CREATED, ORDER_UPDATED, STATUS_CHANGED, ORDER_COMPLETED, ORDER_CANCELLED
 */
public class LogDao {

    /** Adds a log that is not about an order (login, logout, account actions). */
    public static void logAction(int userId, String actionType, String description) throws SQLException {
        logAction(userId, null, actionType, description);
    }

    /** Adds a log. orderId can be null. Uses a parameterized PreparedStatement. */
    public static void logAction(int userId, Integer orderId, String actionType, String description)
            throws SQLException {
        String sql = "INSERT INTO activity_logs(user_id, order_id, action_type, description, timestamp) "
                + "VALUES(?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (orderId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, orderId);
            ps.setString(3, actionType);
            ps.setString(4, description);
            ps.setString(5, now());
            ps.executeUpdate();
        }
    }

    /** Every recorded action, newest first. */
    public static List<ActivityLog> getAllLogs() throws SQLException {
        return query(" ORDER BY l.timestamp DESC, l.id DESC", null);
    }

    /** Every action done by one user, newest first. */
    public static List<ActivityLog> getLogsByUserId(int userId) throws SQLException {
        return query(" WHERE l.user_id = ? ORDER BY l.timestamp DESC, l.id DESC", userId);
    }

    // The text after "suffix" is written in this file; the user ID is only passed as a parameter.
    private static List<ActivityLog> query(String suffix, Integer userId) throws SQLException {
        String sql = "SELECT l.id, l.user_id, u.username, l.order_id, l.action_type, l.description, l.timestamp "
                + "FROM activity_logs l LEFT JOIN UserAccount u ON u.user_id = l.user_id" + suffix;
        List<ActivityLog> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int oid = rs.getInt("order_id");
                    Integer orderId = rs.wasNull() ? null : oid;
                    list.add(new ActivityLog(rs.getInt("id"), rs.getInt("user_id"), rs.getString("username"),
                            orderId, rs.getString("action_type"), rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        }
        return list;
    }
}
