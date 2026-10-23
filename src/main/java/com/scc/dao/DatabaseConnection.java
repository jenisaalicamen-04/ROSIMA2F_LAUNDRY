package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:laundryOrders.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static void initialize() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // TABLE 1: Customer
            st.execute("""
                    CREATE TABLE IF NOT EXISTS Customer (
                        customer_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        name            TEXT NOT NULL,
                        contact_number  TEXT NOT NULL,
                        address         TEXT NOT NULL
                    )""");

            // TABLE 3: User
            st.execute("""
                    CREATE TABLE IF NOT EXISTS UserAccount (
                        user_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        username        TEXT NOT NULL UNIQUE,
                        password        TEXT NOT NULL,
                        role            TEXT NOT NULL CHECK (role IN ('Customer','Laundry Staff','Admin')),
                        customer_id     INTEGER REFERENCES Customer(customer_id),
                        account_status  TEXT NOT NULL DEFAULT 'PENDING'
                                        CHECK (account_status IN ('PENDING','APPROVED','Inactive'))
                    )""");

            // TABLE 2: LaundryOrder
            st.execute("""
                    CREATE TABLE IF NOT EXISTS LaundryOrder (
                        order_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_id   INTEGER NOT NULL REFERENCES Customer(customer_id),
                        processed_by  INTEGER REFERENCES UserAccount(user_id),
                        service_type  TEXT NOT NULL,
                        order_date    TEXT NOT NULL,
                        status        TEXT NOT NULL DEFAULT 'Pending'
                                      CHECK (status IN ('Pending','Processing','Ready','Completed','Cancelled')),
                        total_amount  REAL NOT NULL DEFAULT 0
                    )""");

            // TABLE 4: ActivityLog
            st.execute("""
                    CREATE TABLE IF NOT EXISTS ActivityLog (
                        log_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id       INTEGER NOT NULL REFERENCES UserAccount(user_id),
                        order_id      INTEGER REFERENCES LaundryOrder(order_id),
                        action        TEXT NOT NULL,
                        log_datetime  TEXT NOT NULL
                    )""");
        }
        UserDao.seedDefaultAccounts();
    }
}
