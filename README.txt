LAUNDRY ORDER TRACKING SYSTEM  (PF201: OOP - IT2G)

HOW TO RUN IN INTELLIJ
1. File > Open > select the "laundryOrderTrackingSystem" folder (it has pom.xml).
2. Click "Load Maven Changes" if IntelliJ asks. If your JDK is not 26, change the two
   "26" values in pom.xml to your JDK version.
3. Run > Edit Configurations > select Main > Modify options > Add VM options, then type:
       --enable-native-access=ALL-UNNAMED
   (this removes the "restricted method in java.lang.System" warnings)
4. Run com.scc.Main

LOGINS
   Admin : admin / admin123
   Staff : staff / staff123
   Customers: choose [2] Register as Customer on the first menu. New accounts are PENDING
              until the Admin approves them (Admin menu > [9]).

DATABASE (SQLite - open it in Letos)
   File: laundryOrders.db (project root, next to pom.xml)
   Tables: Customer, LaundryOrder, UserAccount, ActivityLog
   Passwords are stored as typed (plain text, not hashed) in UserAccount.password.
   The program creates the file by itself if it is missing.
   Close the file in Letos before running the program if you see "database is locked".

REPORT EXPORTS (Admin menu > [6])
   Saved in the "exports" folder: PDF, CSV and XLSX.

PROJECT LAYOUT AND FLOW
   Main.java            entry point only: creates the database, then opens MainMenu
   ui/MainMenu          [1] Login  [2] Register  [0] Exit   (logout always returns here)
   ui/CustomerMenu      customer screen
   ui/StaffMenu         laundry staff screen
   ui/AdminMenu         admin screen (orders, activity log, report + export)
   ui/AccountMenu       admin sub-screen: manage accounts
   ui/OrderSearch       search screen used by staff and admin
   dao/                 DatabaseConnection, UserDao, CustomerDao, OrderDao, LogDao (all SQL is here)
   model/               User, Customer, LaundryOrder, ActivityLog, StatusReport (Java records)
   util/                Constants, Console (input/print helpers), ReportExporter (PDF/CSV/XLSX)
