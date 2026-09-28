package com.rental.gui;

import com.rental.model.*;
import com.rental.service.RentalAdmin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Graphical User Interface for the Vehicle Rental System built with Java Swing.
 * Connects directly to RentalAdmin service and models.
 */
public class RentalSystemGUI extends JFrame {

    private final RentalAdmin admin;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Table Models
    private DefaultTableModel vehicleTableModel;
    private DefaultTableModel customerTableModel;
    private DefaultTableModel rentalTableModel;
    private DefaultTableModel historyTableModel;

    // Component References
    private JComboBox<String> bookCustomerCombo;
    private JComboBox<String> bookVehicleCombo;
    private JTextField bookStartDateField;
    private JTextField bookEndDateField;
    private JTextField bookDepositField;

    private JComboBox<String> returnRentalCombo;
    private JTextField returnDateField;
    private JLabel returnSummaryLabel;

    public RentalSystemGUI() {
        this.admin = new RentalAdmin();
        seedData();
        initUI();
    }

    private void seedData() {
        admin.addVehicle(new Car("V001", "Porsche", "Taycan 4S", 4500.0, 5, "Electric"));
        admin.addVehicle(new Bike("V002", "Ducati", "Panigale V4 S", 2200.0, 1103, true));
        admin.addVehicle(new Van("V003", "Ford", "Transit Custom", 1800.0, 1400.0, true));
        admin.addVehicle(new Car("V004", "Tesla", "Model S Plaid", 3800.0, 5, "Electric"));
        admin.addVehicle(new Bike("V005", "BMW", "S1000RR", 2100.0, 999, true));
        admin.addVehicle(new Van("V006", "Toyota", "Innova Crysta", 2400.0, 850.0, false));
        admin.addVehicle(new Car("V007", "Mercedes-Benz", "E-Class", 3600.0, 5, "Petrol"));

        admin.addCustomer(new Customer("C001", "Anjali Menon", "anjali@example.com", "DL-0420180012345"));
        admin.addCustomer(new Customer("C002", "Rohan Das", "rohan@example.com", "DL-0720190067890"));
        admin.addCustomer(new Customer("C003", "Elena Vance", "elena.vance@velocita.com", "DL-1220210045678"));

        // Create a sample booking
        Customer c = admin.findCustomerById("C001");
        Vehicle v = admin.getAllVehicles().get(0);
        if (c != null && v != null) {
            admin.bookVehicle(c, v, LocalDate.now().minusDays(5), LocalDate.now().plusDays(2), 5000.0);
        }
    }

    private void initUI() {
        setTitle("Velocita Fleet Management & Vehicle Rental System");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Apply dark/modern look if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Main Layout Container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(248, 250, 252)); // Clean light canvas

        // Header Banner
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed View
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("🚗 Fleet Inventory", createInventoryPanel());
        tabbedPane.addTab("🔑 Book a Vehicle", createBookingPanel());
        tabbedPane.addTab("🔄 Return Processing", createReturnPanel());
        tabbedPane.addTab("👥 Customer Directory", createCustomerPanel());
        tabbedPane.addTab("📊 Active Leases & History", createRentalHistoryPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Status Bar Footer
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Refresh dynamic UI elements
        refreshAllData();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(16, 25, 16, 25)
        ));

        JLabel titleLabel = new JLabel("⚡ VELOCITA FLEET MANAGEMENT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(15, 23, 42)); // Deep slate

        JLabel subLabel = new JLabel("Real-time Vehicle Rental & Dispatch System | GUI Mode");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(100, 116, 139));

        JPanel textGroup = new JPanel(new GridLayout(2, 1, 0, 4));
        textGroup.setOpaque(false);
        textGroup.add(titleLabel);
        textGroup.add(subLabel);

        header.add(textGroup, BorderLayout.WEST);

        JLabel badgeLabel = new JLabel("SYSTEM ONLINE");
        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(new Color(236, 253, 245));
        badgeLabel.setForeground(new Color(5, 150, 105));
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(167, 243, 208), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));

        header.add(badgeLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JLabel status = new JLabel("Ready. Connected to RentalAdmin Service.");
        status.setForeground(new Color(100, 116, 139));
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.add(status);
        return footer;
    }

    // =========================================================
    // TAB 1: FLEET INVENTORY
    // =========================================================

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addBtn = new JButton("+ Add Vehicle");
        styleButton(addBtn, new Color(16, 185, 129));
        addBtn.addActionListener(e -> showAddVehicleDialog());

        JButton updateBtn = new JButton("Update Selected");
        styleButton(updateBtn, new Color(59, 130, 246));
        updateBtn.addActionListener(e -> showUpdateVehicleDialog());

        JButton deleteBtn = new JButton("Remove Vehicle");
        styleButton(deleteBtn, new Color(239, 68, 68));
        deleteBtn.addActionListener(e -> handleRemoveVehicle());

        JTextField searchField = new JTextField(15);
        JButton searchBtn = new JButton("Search Brand/Type");
        styleButton(searchBtn, new Color(107, 114, 128));
        searchBtn.addActionListener(e -> handleSearchVehicle(searchField.getText().trim()));

        JButton refreshBtn = new JButton("Reset / Show All");
        styleButton(refreshBtn, new Color(71, 85, 105));
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            refreshVehicleTable(admin.getAllVehicles());
        });

        JButton fitPriceBtn = new JButton("💲 Fit Price");
        styleButton(fitPriceBtn, new Color(245, 158, 11));
        fitPriceBtn.addActionListener(e -> showFitPriceDialog());

        toolbar.add(addBtn);
        toolbar.add(fitPriceBtn);
        toolbar.add(updateBtn);
        toolbar.add(deleteBtn);
        toolbar.add(new JLabel("  Filter:"));
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);

        panel.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Vehicle ID", "Type", "Brand", "Model", "Daily Rate ($)", "Availability Status"};
        vehicleTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(vehicleTableModel);
        formatTable(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // TAB 2: BOOK A VEHICLE
    // =========================================================

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("🔑 Reserve & Book Fleet Asset");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(heading, gbc);

        gbc.gridwidth = 1;

        // Customer
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Select Customer:"), gbc);
        bookCustomerCombo = new JComboBox<>();
        gbc.gridx = 1;
        panel.add(bookCustomerCombo, gbc);

        // Vehicle
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Select Available Vehicle:"), gbc);
        bookVehicleCombo = new JComboBox<>();
        gbc.gridx = 1;
        panel.add(bookVehicleCombo, gbc);

        // Start Date
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Start Date (yyyy-MM-dd):"), gbc);
        bookStartDateField = new JTextField(LocalDate.now().toString());
        gbc.gridx = 1;
        panel.add(bookStartDateField, gbc);

        // End Date
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("End Date (yyyy-MM-dd):"), gbc);
        bookEndDateField = new JTextField(LocalDate.now().plusDays(3).toString());
        gbc.gridx = 1;
        panel.add(bookEndDateField, gbc);

        // Security Deposit
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Security Deposit ($):"), gbc);
        bookDepositField = new JTextField("200.00");
        gbc.gridx = 1;
        panel.add(bookDepositField, gbc);

        // Submit Button
        JButton bookBtn = new JButton("Confirm Booking");
        styleButton(bookBtn, new Color(16, 185, 129));
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(bookBtn, gbc);

        bookBtn.addActionListener(e -> handleBookVehicle());

        return panel;
    }

    // =========================================================
    // TAB 3: RETURN PROCESSING
    // =========================================================

    private JPanel createReturnPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("🔄 Vehicle Return & Deposit Settlement");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(heading, gbc);

        gbc.gridwidth = 1;

        // Rental Combo
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Active Rental Transaction:"), gbc);
        returnRentalCombo = new JComboBox<>();
        gbc.gridx = 1;
        panel.add(returnRentalCombo, gbc);

        // Actual Return Date
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Actual Return Date (yyyy-MM-dd):"), gbc);
        returnDateField = new JTextField(LocalDate.now().toString());
        gbc.gridx = 1;
        panel.add(returnDateField, gbc);

        // Summary box
        returnSummaryLabel = new JLabel("Select an active rental to process settlement.");
        returnSummaryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(returnSummaryLabel, gbc);

        // Submit Button
        JButton returnBtn = new JButton("Authorize Return & Issue Refund");
        styleButton(returnBtn, new Color(59, 130, 246));
        returnBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridy = 4;
        panel.add(returnBtn, gbc);

        returnBtn.addActionListener(e -> handleReturnVehicle());

        return panel;
    }

    // =========================================================
    // TAB 4: CUSTOMER DIRECTORY
    // =========================================================

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton addCustBtn = new JButton("+ Register New Customer");
        styleButton(addCustBtn, new Color(16, 185, 129));
        addCustBtn.addActionListener(e -> showAddCustomerDialog());

        toolbar.add(addCustBtn);
        panel.add(toolbar, BorderLayout.NORTH);

        String[] cols = {"Customer ID", "Full Name", "Contact Details", "Driving Licence", "Total Rentals Completed"};
        customerTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(customerTableModel);
        formatTable(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // TAB 5: LEASE HISTORY
    // =========================================================

    private JPanel createRentalHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Rental ID", "Customer", "Vehicle", "Start Date", "Scheduled End", "Cost ($)", "Deposit ($)", "Late Penalty ($)", "Status"};
        rentalTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(rentalTableModel);
        formatTable(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // ACTION HANDLERS & DIALOGS
    // =========================================================

    private void showAddVehicleDialog() {
        JTextField idField = new JTextField("V00" + (admin.getAllVehicles().size() + 1));
        JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
        JTextField brandField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField rateField = new JTextField("100.00");

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Vehicle ID:")); form.add(idField);
        form.add(new JLabel("Type:")); form.add(typeCombo);
        form.add(new JLabel("Brand:")); form.add(brandField);
        form.add(new JLabel("Model:")); form.add(modelField);
        form.add(new JLabel("Daily Rate ($):")); form.add(rateField);

        int res = JOptionPane.showConfirmDialog(this, form, "Add New Vehicle", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String id = idField.getText().trim();
                VehicleType type = (VehicleType) typeCombo.getSelectedItem();
                String brand = brandField.getText().trim();
                String model = modelField.getText().trim();
                double rate = Double.parseDouble(rateField.getText().trim());

                if (id.isEmpty() || brand.isEmpty() || model.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                admin.addVehicle(new Vehicle(id, type, brand, model, rate));
                refreshAllData();
                JOptionPane.showMessageDialog(this, "Vehicle added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid inputs: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showUpdateVehicleDialog() {
        String id = JOptionPane.showInputDialog(this, "Enter Vehicle ID to update:");
        if (id == null || id.trim().isEmpty()) return;

        Vehicle v = admin.getAllVehicles().stream()
                .filter(veh -> veh.getVehicleId().equalsIgnoreCase(id.trim()))
                .findFirst().orElse(null);

        if (v == null) {
            JOptionPane.showMessageDialog(this, "Vehicle not found with ID: " + id, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField brandField = new JTextField(v.getBrand());
        JTextField modelField = new JTextField(v.getModel());

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("New Brand:")); form.add(brandField);
        form.add(new JLabel("New Model:")); form.add(modelField);

        int res = JOptionPane.showConfirmDialog(this, form, "Update Vehicle Details (" + id + ")", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                admin.updateVehicle(id, brandField.getText().trim(), modelField.getText().trim(), v.getRentalRate());
                refreshAllData();
                JOptionPane.showMessageDialog(this, "Vehicle details updated successfully!\n(Note: Use 'Fit Price' to adjust rental rates.)", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating vehicle: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showFitPriceDialog() {
        String id = JOptionPane.showInputDialog(this, "Enter Vehicle ID to fit/set custom price (e.g. V001):");
        if (id == null || id.trim().isEmpty()) return;

        Vehicle v = admin.findVehicleById(id.trim());
        if (v == null) {
            JOptionPane.showMessageDialog(this, "Vehicle not found with ID: " + id, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                String.format("Vehicle: %s %s (Current: ₹%.2f/day)\nEnter New Daily Rental Rate (₹):",
                        v.getBrand(), v.getModel(), v.getRentalRate()),
                String.valueOf(v.getRentalRate()));

        if (input == null || input.trim().isEmpty()) return;

        try {
            double newRate = Double.parseDouble(input.trim());
            if (newRate <= 0) {
                JOptionPane.showMessageDialog(this, "Price must be a positive number.", "Invalid Rate", JOptionPane.ERROR_MESSAGE);
                return;
            }

            v.setRentalRate(newRate);
            refreshAllData();
            JOptionPane.showMessageDialog(this,
                    String.format("Price fitted successfully!\n%s %s is now set to ₹%.2f / day.",
                            v.getBrand(), v.getModel(), newRate),
                    "Price Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid price format. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRemoveVehicle() {
        String id = JOptionPane.showInputDialog(this, "Enter Vehicle ID to remove:");
        if (id == null || id.trim().isEmpty()) return;

        boolean success = admin.removeVehicle(id.trim());
        if (success) {
            refreshAllData();
            JOptionPane.showMessageDialog(this, "Vehicle " + id + " removed.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Could not remove vehicle (it may be currently rented or non-existent).", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSearchVehicle(String query) {
        if (query.isEmpty()) {
            refreshVehicleTable(admin.getAllVehicles());
            return;
        }

        List<Vehicle> matches = admin.searchByBrand(query);
        try {
            VehicleType type = VehicleType.valueOf(query.toUpperCase());
            matches.addAll(admin.searchByType(type));
        } catch (IllegalArgumentException ignored) {}

        refreshVehicleTable(matches);
    }

    private void showAddCustomerDialog() {
        JTextField idField = new JTextField("C00" + (admin.getAllCustomers().size() + 1));
        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField licenceField = new JTextField("DL-");

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Customer ID:")); form.add(idField);
        form.add(new JLabel("Full Name:")); form.add(nameField);
        form.add(new JLabel("Contact Email/Phone:")); form.add(contactField);
        form.add(new JLabel("Driving Licence Number:")); form.add(licenceField);

        int res = JOptionPane.showConfirmDialog(this, form, "Register New Customer", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String contact = contactField.getText().trim();
            String licence = licenceField.getText().trim();

            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID and Name are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (licence.replaceAll("[^a-zA-Z0-9]", "").length() < 6) {
                JOptionPane.showMessageDialog(this, "A valid driving licence number (at least 6 alphanumeric characters) is required for verification.", "Licence Verification Failed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            admin.addCustomer(new Customer(id, name, contact, licence));
            refreshAllData();
            JOptionPane.showMessageDialog(this, "Customer registered and driving licence verified successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleBookVehicle() {
        try {
            String custItem = (String) bookCustomerCombo.getSelectedItem();
            String vehItem = (String) bookVehicleCombo.getSelectedItem();

            if (custItem == null || vehItem == null) {
                JOptionPane.showMessageDialog(this, "Select a valid customer and vehicle!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String custId = custItem.split(" - ")[0];
            String vehId = vehItem.split(" - ")[0];

            Customer customer = admin.findCustomerById(custId);
            Vehicle vehicle = admin.getAllVehicles().stream()
                    .filter(v -> v.getVehicleId().equalsIgnoreCase(vehId))
                    .findFirst().orElse(null);

            LocalDate start = LocalDate.parse(bookStartDateField.getText().trim(), DATE_FORMAT);
            LocalDate end = LocalDate.parse(bookEndDateField.getText().trim(), DATE_FORMAT);
            double deposit = Double.parseDouble(bookDepositField.getText().trim());

            Rental rental = admin.bookVehicle(customer, vehicle, start, end, deposit);
            if (rental != null) {
                refreshAllData();
                JOptionPane.showMessageDialog(this, "Booking Successful!\n" + rental, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Booking failed. Vehicle may not be available or date range invalid.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid form data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReturnVehicle() {
        try {
            String rentalItem = (String) returnRentalCombo.getSelectedItem();
            if (rentalItem == null) {
                JOptionPane.showMessageDialog(this, "No active rental selected!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String rentalId = rentalItem.split(" - ")[0];
            LocalDate returnDate = LocalDate.parse(returnDateField.getText().trim(), DATE_FORMAT);

            double refund = admin.returnVehicle(rentalId, returnDate);
            if (refund >= 0) {
                refreshAllData();
                JOptionPane.showMessageDialog(this, String.format("Return Authorized & Processed!\nNet Deposit Refund Issued: $%.2f", refund), "Return Complete", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Return failed. Rental record not found or already closed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing return: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // UI UTILITIES & REFRESH LOGIC
    // =========================================================

    private void refreshAllData() {
        refreshVehicleTable(admin.getAllVehicles());
        refreshCustomerTable();
        refreshRentalTable();
        refreshCombos();
    }

    private void refreshVehicleTable(List<Vehicle> list) {
        vehicleTableModel.setRowCount(0);
        for (Vehicle v : list) {
            vehicleTableModel.addRow(new Object[]{
                    v.getVehicleId(), v.getType(), v.getBrand(), v.getModel(),
                    String.format("%.2f", v.getRentalRate()), v.getAvailabilityStatus()
            });
        }
    }

    private void refreshCustomerTable() {
        customerTableModel.setRowCount(0);
        for (Customer c : admin.getAllCustomers()) {
            customerTableModel.addRow(new Object[]{
                    c.getCustomerId(), c.getName(), c.getContactDetails(), c.getLicenceNumber(), c.getRentalHistory().size()
            });
        }
    }

    private void refreshRentalTable() {
        rentalTableModel.setRowCount(0);
        for (Rental r : admin.getAllRentals()) {
            rentalTableModel.addRow(new Object[]{
                    r.getRentalId(), r.getCustomer().getName(),
                    r.getVehicle().getBrand() + " " + r.getVehicle().getModel(),
                    r.getStartDate(), r.getEndDate(),
                    String.format("%.2f", r.getRentalCost()),
                    String.format("%.2f", r.getSecurityDeposit()),
                    String.format("%.2f", r.getLateReturnPenalty()),
                    r.isActive() ? "ACTIVE" : "RETURNED"
            });
        }
    }

    private void refreshCombos() {
        if (bookCustomerCombo != null) {
            bookCustomerCombo.removeAllItems();
            for (Customer c : admin.getAllCustomers()) {
                bookCustomerCombo.addItem(c.getCustomerId() + " - " + c.getName());
            }
        }

        if (bookVehicleCombo != null) {
            bookVehicleCombo.removeAllItems();
            for (Vehicle v : admin.getAllVehicles()) {
                if (v.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
                    bookVehicleCombo.addItem(v.getVehicleId() + " - " + v.getBrand() + " " + v.getModel() + " ($" + v.getRentalRate() + "/day)");
                }
            }
        }

        if (returnRentalCombo != null) {
            returnRentalCombo.removeAllItems();
            for (Rental r : admin.getAllRentals()) {
                if (r.isActive()) {
                    returnRentalCombo.addItem(r.getRentalId() + " - " + r.getCustomer().getName() + " (" + r.getVehicle().getModel() + ")");
                }
            }
        }
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
    }

    private void formatTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RentalSystemGUI gui = new RentalSystemGUI();
            gui.setVisible(true);
        });
    }
}
