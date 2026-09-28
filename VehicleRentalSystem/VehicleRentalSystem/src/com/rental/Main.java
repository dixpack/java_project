package com.rental;

import com.rental.model.*;
import com.rental.service.RentalAdmin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console entry point. Provides a simple numbered menu so the whole
 * system can be exercised interactively without any extra UI framework.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final RentalAdmin admin = new RentalAdmin();

    public static void main(String[] args) {
        seedSampleData(); // a couple of starter vehicles/customers so the menu isn't empty on first run
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1: addVehicleFlow(); break;
                case 2: removeVehicleFlow(); break;
                case 3: updateVehicleFlow(); break;
                case 4: searchVehicleFlow(); break;
                case 5: bookVehicleFlow(); break;
                case 6: returnVehicleFlow(); break;
                case 7: viewAllVehicles(); break;
                case 8: viewCustomerHistory(); break;
                case 9: addCustomerFlow(); break;
                case 10: launchGuiFlow(); break;
                case 11: launchWebServerFlow(); break;
                case 12: runTestSuiteFlow(); break;
                case 13: running = false; System.out.println("Exiting. Goodbye!"); break;
                default: System.out.println("Invalid choice. Please try again.");
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n===== VEHICLE RENTAL SYSTEM =====");
        System.out.println("1. Add Vehicle");
        System.out.println("2. Remove Vehicle");
        System.out.println("3. Update Vehicle");
        System.out.println("4. Search Vehicles (by type/brand)");
        System.out.println("5. Book a Vehicle");
        System.out.println("6. Return a Vehicle (Calculate late fee & deposit refund)");
        System.out.println("7. View All Vehicles");
        System.out.println("8. View Customer Rental History");
        System.out.println("9. Register a New Customer");
        System.out.println("10. Launch Desktop GUI (Swing)");
        System.out.println("11. Launch Embedded Web Server & HTML UI (http://localhost:8080)");
        System.out.println("12. Run Automated Test Suite (RentalSystemTest)");
        System.out.println("13. Exit");
    }

    // =========================================================
    // MENU ACTION HANDLERS
    // =========================================================

    private static void addVehicleFlow() {
        String id = readLine("Vehicle ID: ");
        VehicleType type = readVehicleType();
        String brand = readLine("Brand: ");
        String model = readLine("Model: ");
        double rate = readDouble("Rental rate per day: ");
        admin.addVehicle(new Vehicle(id, type, brand, model, rate));
    }

    private static void removeVehicleFlow() {
        String id = readLine("Vehicle ID to remove: ");
        admin.removeVehicle(id);
    }

    private static void updateVehicleFlow() {
        String id = readLine("Vehicle ID to update: ");
        String brand = readLine("New brand (leave blank to skip): ");
        String model = readLine("New model (leave blank to skip): ");
        String rateStr = readLine("New rate (leave blank to skip): ");
        double rate = rateStr.isBlank() ? -1 : Double.parseDouble(rateStr);
        admin.updateVehicle(id, brand.isBlank() ? null : brand, model.isBlank() ? null : model, rate);
    }

    private static void searchVehicleFlow() {
        System.out.println("Search by: 1) Type  2) Brand");
        int opt = readInt("Choice: ");
        List<Vehicle> results;
        if (opt == 1) {
            VehicleType type = readVehicleType();
            results = admin.searchByType(type);
        } else {
            String brand = readLine("Brand: ");
            results = admin.searchByBrand(brand);
        }
        if (results.isEmpty()) {
            System.out.println("No available vehicles match that search.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private static void bookVehicleFlow() {
        String customerId = readLine("Customer ID: ");
        Customer customer = admin.findCustomerById(customerId);
        if (customer == null) {
            System.out.println("Error: No customer found with ID " + customerId + ". Register them first (option 9).");
            return;
        }
        String vehicleId = readLine("Vehicle ID: ");
        Vehicle vehicle = admin.getAllVehicles().stream()
                .filter(v -> v.getVehicleId().equalsIgnoreCase(vehicleId))
                .findFirst().orElse(null);
        if (vehicle == null) {
            System.out.println("Error: No vehicle found with ID " + vehicleId);
            return;
        }
        LocalDate start = readDate("Start date (yyyy-MM-dd): ");
        LocalDate end = readDate("End date (yyyy-MM-dd): ");
        double deposit = readDouble("Security deposit: ");
        admin.bookVehicle(customer, vehicle, start, end, deposit);
    }

    private static void returnVehicleFlow() {
        String rentalId = readLine("Rental ID: ");
        LocalDate actualReturn = readDate("Actual return date (yyyy-MM-dd): ");
        admin.returnVehicle(rentalId, actualReturn);
    }

    private static void viewAllVehicles() {
        if (admin.getAllVehicles().isEmpty()) {
            System.out.println("No vehicles in inventory.");
        } else {
            admin.getAllVehicles().forEach(System.out::println);
        }
    }

    private static void viewCustomerHistory() {
        String customerId = readLine("Customer ID: ");
        Customer customer = admin.findCustomerById(customerId);
        if (customer == null) {
            System.out.println("Error: No customer found with ID " + customerId);
            return;
        }
        if (customer.getRentalHistory().isEmpty()) {
            System.out.println(customer.getName() + " has no rental history yet.");
        } else {
            customer.getRentalHistory().forEach(System.out::println);
        }
    }

    private static void addCustomerFlow() {
        String id = readLine("Customer ID: ");
        String name = readLine("Name: ");
        String contact = readLine("Contact details: ");
        admin.addCustomer(new Customer(id, name, contact));
    }

    private static void launchGuiFlow() {
        System.out.println("Launching Desktop GUI...");
        try {
            javax.swing.SwingUtilities.invokeLater(() -> {
                com.rental.gui.RentalSystemGUI gui = new com.rental.gui.RentalSystemGUI();
                gui.setVisible(true);
            });
            System.out.println("GUI window launched in background.");
        } catch (Exception e) {
            System.err.println("Could not launch GUI in this environment: " + e.getMessage());
        }
    }

    private static void launchWebServerFlow() {
        System.out.println("Starting Embedded Web Server at http://localhost:8080/ ...");
        try {
            java.nio.file.Path webDir = java.nio.file.Paths.get("web");
            if (!java.nio.file.Files.exists(webDir)) webDir = java.nio.file.Paths.get("../web");
            if (!java.nio.file.Files.exists(webDir)) webDir = java.nio.file.Paths.get("../../web");
            com.rental.server.RentalHttpServer server = new com.rental.server.RentalHttpServer(admin, webDir);
            server.start();
            System.out.println("Web server is running! Open http://localhost:8080/ in your browser.");
        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
        }
    }

    private static void runTestSuiteFlow() {
        System.out.println("Running automated tests...");
        com.rental.test.RentalSystemTest.main(new String[0]);
    }

    // =========================================================
    // SAMPLE DATA (so the menu has something to work with immediately)
    // =========================================================

    private static void seedSampleData() {
        admin.seedDefaultData();
        System.out.println("(Sample vehicles V001-V007 and customers C001-C003 pre-loaded.)");
    }

    // =========================================================
    // INPUT HELPERS (keep parsing/validation out of the business logic)
    // =========================================================

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return LocalDate.parse(scanner.nextLine().trim(), DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Please enter a date in yyyy-MM-dd format (e.g. 2026-07-20).");
            }
        }
    }

    private static VehicleType readVehicleType() {
        while (true) {
            String input = readLine("Type (CAR / BIKE / VAN): ").toUpperCase();
            try {
                return VehicleType.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Please enter one of: CAR, BIKE, VAN.");
            }
        }
    }
}
