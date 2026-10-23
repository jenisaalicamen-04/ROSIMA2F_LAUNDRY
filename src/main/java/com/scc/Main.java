package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.ui.MainMenu;

import java.sql.SQLException;

/**
 * LAUNDRY ORDER TRACKING SYSTEM  (PF201: OOP - IT2G) - app entry point.
 *
 * Flow:  Main -> DatabaseConnection.initialize() -> MainMenu -> role menu -> back to MainMenu
 *
 * Default accounts (created automatically on the first run):
 *   Admin : admin / admin123
 *   Staff : staff / staff123
 */
public class 1Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection.initialize();   // creates the .db file + tables if missing
        } catch (SQLException e) {
            System.out.println("Cannot open the database: " + e.getMessage());
            return;
        }
        MainMenu.show();
    }
}
