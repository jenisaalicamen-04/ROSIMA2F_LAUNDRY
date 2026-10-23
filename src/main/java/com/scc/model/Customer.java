package com.scc.model;

/** Row of the Customer table. */
public record Customer(int customerId, String name, String contactNumber, String address) {
}
