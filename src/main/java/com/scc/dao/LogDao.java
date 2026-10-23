package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static com.scc.util.Constants.now;

public class LogDao {
    public static void log(int userId, Integer orderId, String action) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO ActivityLog(user_id, order_id, action, log_datetime) VALUES(?,?,?,?)")) {
            ps.setInt(1, userId);
            if (orderId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, orderId);
            ps.setString(3, action);
            ps.setString(4, now());
            ps.executeUpdate();
        }
    }

    public static List<ActivityLog> findAll() throws SQLException {
        List<ActivityLog> list = new ArrayList<>();
        String sql = "SELECT l.log_id, u.username, l.order_id, l.action, l.log_datetime "
                + "FROM ActivityLog l JOIN UserAccount u ON u.user_id = l.user_id ORDER BY l.log_id DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int oid = rs.getInt(3);
                Integer orderId = rs.wasNull() ? null : oid;
                list.add(new ActivityLog(rs.getInt(1), rs.getString(2), orderId, rs.getString(4), rs.getString(5)));
            }
        }
        return list;
    }
}
