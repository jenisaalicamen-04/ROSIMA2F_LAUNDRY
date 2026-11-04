package com.scc.model;

/** Row of the LaundryOrder table (plus the customer's name for display). */
public record LaundryOrder(int orderId, int customerId, String customerName, Integer processedBy,
                           String serviceType, String orderDate, String status, double totalAmount) {
}
