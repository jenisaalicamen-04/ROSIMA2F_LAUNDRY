package com.scc.model;

/** Row of the UserAccount table. */
public record User(int userId, String username, String role, Integer customerId, String accountStatus) {
}
