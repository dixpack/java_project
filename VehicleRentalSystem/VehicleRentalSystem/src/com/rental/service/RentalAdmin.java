package com.rental.service;

import com.rental.interfaces.InventoryManageable;
import com.rental.interfaces.Rentable;
import com.rental.interfaces.Searchable;
import com.rental.model.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central controller of the system. Manages vehicle inventory, customer
 * records, and rental transactions. Implements Rentable, Searchable,
 * and InventoryManageable to fulfill OOP interface segregation and implementation principles.
 */
public class RentalAdmin implements Rentable, Searchable, InventoryManageable {

    private List<Vehicle> vehicles;
    private List<Customer> customers;
    private List<Rental> rentals;

    // Flat daily late fee applied per day a vehicle is returned after its scheduled end date.
    public static final double LATE_FEE_PER_DAY = 500.0;

    private int rentalCounter = 1; // auto-incrementing rental ID counter

    public RentalAdmin() {
        this.vehicles = new ArrayList<>();
        this.customers = new ArrayList<>();
        this.rentals = new ArrayList<>();
    }

    // =========================================================
    // INVENTORY MANAGEMENT (InventoryManageable Implementation)
    // =========================================================

    /** Adds a new vehicle to the inventory. */
    @Override
    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            System.out.println("Error: Cannot add null vehicle.");
            return;
        }
        if (findVehicleById(vehicle.getVehicleId()) != null) {
            System.out.println("Error: Vehicle ID " + vehicle.getVehicleId() + " already exists.");
            return;
        }
        vehicles.add(vehicle);
        System.out.println("Vehicle added successfully: " + vehicle);
    }

    /**
     * Removes a vehicle from inventory by ID.
     * Policy: A vehicle currently RENTED cannot be removed to prevent orphaned active leases.
     */
    @Override
    public boolean removeVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            System.out.println("Error: No vehicle found with ID " + vehicleId);
            return false;
        }
        if (vehicle.getAvailabilityStatus() == AvailabilityStatus.RENTED) {
            System.out.println("Error: Cannot remove vehicle " + vehicleId + " - it is currently rented out.");
            return false;
        }
        vehicles.remove(vehicle);
        System.out.println("Vehicle removed successfully: " + vehicleId);
        return true;
    }

    /**
     * Updates brand, model, and/or rental rate of an existing vehicle.
     */
    @Override
    public boolean updateVehicle(String vehicleId, String newBrand, String newModel, double newRate) {
        return updateVehicle(vehicleId, null, newBrand, newModel, newRate);
    }

    /**
     * Overloaded update supporting type, brand, model, and rate changes.
     */
    public boolean updateVehicle(String vehicleId, VehicleType newType, String newBrand, String newModel, double newRate) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            System.out.println("Error: No vehicle found with ID " + vehicleId);
            return false;
        }
        if (newType != null) vehicle.setType(newType);
        if (newBrand != null && !newBrand.trim().isEmpty()) vehicle.setBrand(newBrand.trim());
        if (newModel != null && !newModel.trim().isEmpty()) vehicle.setModel(newModel.trim());
        if (newRate >= 0) vehicle.setRentalRate(newRate);
        System.out.println("Vehicle updated successfully: " + vehicle);
        return true;
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicles;
    }

    @Override
    public Vehicle findVehicleById(String vehicleId) {
        if (vehicleId == null) return null;
        for (Vehicle v : vehicles) {
            if (v.getVehicleId().equalsIgnoreCase(vehicleId.trim())) {
                return v;
            }
        }
        return null;
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(v -> v.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public List<Vehicle> getRentedVehicles() {
        return vehicles.stream()
                .filter(v -> v.getAvailabilityStatus() == AvailabilityStatus.RENTED)
                .collect(Collectors.toList());
    }

    // =========================================================
    // CUSTOMER MANAGEMENT
    // =========================================================

    public void addCustomer(Customer customer) {
        if (customer == null) return;
        if (findCustomerById(customer.getCustomerId()) != null) {
            System.out.println("Error: Customer ID " + customer.getCustomerId() + " already exists.");
            return;
        }
        customers.add(customer);
        System.out.println("Customer registered successfully: " + customer);
    }

    public Customer findCustomerById(String customerId) {
        if (customerId == null) return null;
        for (Customer c : customers) {
            if (c.getCustomerId().equalsIgnoreCase(customerId.trim())) {
                return c;
            }
        }
        return null;
    }

    public List<Customer> getAllCustomers() {
        return customers;
    }

    // =========================================================
    // SEARCHABLE IMPLEMENTATION
    // =========================================================

    @Override
    public List<Vehicle> searchByType(VehicleType type) {
        return vehicles.stream()
                .filter(v -> v.getType() == type && v.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Vehicle> searchByBrand(String brand) {
        if (brand == null) return new ArrayList<>();
        String query = brand.trim().toLowerCase();
        return vehicles.stream()
                .filter(v -> v.getBrand().toLowerCase().contains(query) && v.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    // =========================================================
    // RENTABLE IMPLEMENTATION (booking + returns)
    // =========================================================

    @Override
    public Rental bookVehicle(Customer customer, Vehicle vehicle, LocalDate startDate,
                               LocalDate endDate, double securityDeposit) {

        if (customer == null || vehicle == null || startDate == null || endDate == null) {
            System.out.println("Error: Incomplete booking parameters.");
            return null;
        }

        // Real-time check: must be AVAILABLE
        if (vehicle.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            System.out.println("Error: Vehicle " + vehicle.getVehicleId() + " is currently " + vehicle.getAvailabilityStatus() + ".");
            return null;
        }

        if (!endDate.isAfter(startDate)) {
            System.out.println("Error: End date must be strictly after start date.");
            return null;
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        double cost = days * vehicle.getRentalRate();

        String rentalId = "R" + String.format("%03d", rentalCounter++);
        Rental rental = new Rental(rentalId, customer, vehicle, startDate, endDate, cost, securityDeposit);

        // Real-time availability tracking: immediately switch status to RENTED
        vehicle.setAvailabilityStatus(AvailabilityStatus.RENTED);

        rentals.add(rental);
        customer.addToHistory(rental);

        System.out.println("Booking successful! " + rental);
        return rental;
    }

    @Override
    public double returnVehicle(String rentalId, LocalDate actualReturnDate) {
        Rental rental = findRentalById(rentalId);
        if (rental == null) {
            System.out.println("Error: No rental found with ID " + rentalId);
            return -1;
        }
        if (!rental.isActive()) {
            System.out.println("Error: Rental " + rentalId + " has already been closed/returned.");
            return -1;
        }
        if (actualReturnDate == null) {
            actualReturnDate = LocalDate.now();
        }

        long daysLate = ChronoUnit.DAYS.between(rental.getEndDate(), actualReturnDate);
        double penalty = 0.0;
        if (daysLate > 0) {
            penalty = daysLate * LATE_FEE_PER_DAY;
        }
        rental.setLateReturnPenalty(penalty);

        // Refund calculation: deposit minus penalty, cannot be negative
        double refund = rental.getSecurityDeposit() - penalty;
        if (refund < 0) {
            refund = 0;
        }

        // Store audit details
        rental.setActualReturnDate(actualReturnDate);
        rental.setRefundAmount(refund);
        rental.setActive(false);

        // Real-time availability tracking: vehicle becomes AVAILABLE immediately
        rental.getVehicle().setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        if (daysLate > 0) {
            System.out.printf("Vehicle returned %d day(s) late. Penalty: %.2f. Refund issued: %.2f%n",
                    daysLate, penalty, refund);
        } else {
            System.out.printf("Vehicle returned on time. Full refund issued: %.2f%n", refund);
        }

        return refund;
    }

    public Rental findRentalById(String rentalId) {
        if (rentalId == null) return null;
        for (Rental r : rentals) {
            if (r.getRentalId().equalsIgnoreCase(rentalId.trim())) {
                return r;
            }
        }
        return null;
    }

    public List<Rental> getAllRentals() {
        return rentals;
    }

    public List<Rental> getActiveRentals() {
        return rentals.stream().filter(Rental::isActive).collect(Collectors.toList());
    }

    public List<Rental> getCompletedRentals() {
        return rentals.stream().filter(r -> !r.isActive()).collect(Collectors.toList());
    }

    public static double getLateFeePerDay() {
        return LATE_FEE_PER_DAY;
    }

    /** Clears all current in-memory lists (useful for test resets). */
    public void clearAll() {
        vehicles.clear();
        customers.clear();
        rentals.clear();
        rentalCounter = 1;
    }

    /** Pre-populates the system with realistic sample data. */
    public void seedDefaultData() {
        clearAll();
        addVehicle(new Car("V001", "Porsche", "Taycan 4S", 4500.0, 5, "Electric"));
        addVehicle(new Bike("V002", "Ducati", "Panigale V4 S", 2200.0, 1103, true));
        addVehicle(new Van("V003", "Ford", "Transit Custom", 1800.0, 1400.0, true));
        addVehicle(new Car("V004", "Tesla", "Model S Plaid", 3800.0, 5, "Electric"));
        addVehicle(new Bike("V005", "BMW", "S1000RR", 2100.0, 999, true));
        addVehicle(new Van("V006", "Toyota", "Innova Crysta", 2400.0, 850.0, false));
        addVehicle(new Car("V007", "Mercedes-Benz", "E-Class", 3600.0, 5, "Petrol"));

        addCustomer(new Customer("C001", "Anjali Menon", "anjali@example.com", "DL-0420180012345"));
        addCustomer(new Customer("C002", "Rohan Das", "rohan@example.com", "DL-0720190067890"));
        addCustomer(new Customer("C003", "Elena Vance", "elena.vance@velocita.com", "DL-1220210045678"));

        // Seed an active rental: Anjali books Taycan V001
        Customer c1 = findCustomerById("C001");
        Vehicle v1 = findVehicleById("V001");
        if (c1 != null && v1 != null) {
            bookVehicle(c1, v1, LocalDate.now().minusDays(3), LocalDate.now().plusDays(2), 5000.0);
        }
    }
}
