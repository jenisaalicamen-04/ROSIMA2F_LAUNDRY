package com.scc.model;

/** Row of the activity_logs table (plus the username for display). orderId is null when it does not apply. */
public record ActivityLog(int logId, int userId, String username, Integer orderId, String actionType,
                          String description, String timestamp) {
}
