package com.scc.model;

/** Row of the ActivityLog table (plus the username for display). */
public record ActivityLog(int logId, String username, Integer orderId, String action, String logDatetime) {
}
