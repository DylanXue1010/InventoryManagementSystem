# Inventory and Sales Management System (Course Assignment) 📚

## 📝 Overview

Welcome to the Inventory and Sales Management System! This Java Swing-based application is a course assignment designed to provide a comprehensive solution for managing products, sales, purchase orders, suppliers, and customer returns. It features user authentication and persists all data locally.

## ✨ Features

* **User Authentication**: Secure login system.
* **Inventory Management**: Add, edit, delete, view, and search products. Includes low stock reporting.
* **Sales Processing**: Create sales, update inventory, and view sales history.
* **Purchase Order (PO) Management**: Create POs, receive items, and manage PO statuses.
* **Supplier Management**: Manage supplier information.
* **Sales Return Management**: Process customer returns and update inventory.
* **Reporting**: Generate reports like Low Stock, Total Inventory Value, Sales by Product, and Sales by Category.
* **Data Persistence**: All data is saved locally in CSV files within a `data` folder.
* **Role-Based Access**: (Implicit) The default admin user has full access.

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) installed.
* IntelliJ IDEA (or another Java IDE, but instructions are for IntelliJ).

### Running the Application (in IntelliJ IDEA)

1.  **Clone/Download Project**: Ensure all project files are in your local workspace.
2.  **Open in IntelliJ IDEA**: Open the project in IntelliJ IDEA.
3.  **Locate `MainInventoryWindow.java`**: Find this file in your project structure. It's the main entry point for the application.
4.  **Run**: Right-click on the `MainInventoryWindow.java` file and select "Run 'MainInventoryWindow.main()'".

### First Launch & Login

* On the first launch (and subsequent launches if the `data` folder is deleted), a `data/` directory will be created in the root of your project directory. This folder will store all application data in CSV files (e.g., `items.csv`, `users.csv`).
* **Default Admin User**: A default administrator account is automatically created if no users exist.
    * **Username**: `admin`
    * **Password**: `admin`
    You **must** use these credentials to log in.

## 🛠️ How to Use

1.  **Login**:
    * When the application starts, the **Login** window will appear.
    * Enter **Username**: `admin` and **Password**: `admin`.

2.  **Main Window (`Inventory and Sales Management System`)**:
    * After successful login, the main application window opens, displaying the current inventory.
    * Use the buttons at the bottom of the window to navigate to different modules.

3.  **Navigating Modules**:

    * **Inventory Management**:
        * **View**: The main table shows inventory items. Click column headers to sort.
        * **Search**: Use the top search bar for items by SKU, name, or category.
        * **Add/Edit/Delete Product**: Use the respective buttons.
        * **Generate Report**: Access various reports.

    * **Sales**:
        * **Make New Sale**: Opens the sales processing window.
        * **View/Search Sales**: Opens a window to view past sales. From here, you can initiate a **return** for a selected *completed* sale.

    * **Purchase Orders (POs)**:
        * **Create Purchase Order**: Opens the PO creation window.
        * **View Purchase Orders**: View POs, receive items, or cancel POs.

    * **Sales Returns**:
        * Initiate returns via the "View/Search Sales" window by selecting a completed sale.
        * (It is assumed there's also a way to view all past returns, likely through a dedicated "View Sales Returns" window accessed from the main window or a menu if implemented). *Correction: `ViewSalesReturnsWindow.java` allows viewing all returns.*

4.  **Data Interaction**:
    * Forms use text fields for data entry.
    * Dialog boxes confirm actions or display messages.

5.  **Logging Out / Exiting**:
    * "Logout" returns to the Login screen.
    * Closing the main application window (clicking the 'X') will prompt for confirmation. Confirming will save all current data to the CSV files in the `data/` directory before exiting.

## 💾 Data Storage

* All application data (products, user accounts, sales, orders, suppliers, returns) is stored in **CSV (Comma Separated Values) files**.
* These files are located in a `data/` subdirectory automatically created in your project's root directory.
* The system loads from these files on startup and saves to them when the application is properly closed.

---

