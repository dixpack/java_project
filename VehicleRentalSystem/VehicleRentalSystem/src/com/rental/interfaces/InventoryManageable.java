package com.rental.interfaces;

import com.rental.model.Vehicle;
import java.util.List;

/**
 * Contract for managing vehicle inventory operations.
 * Segregating inventory management from rental transactions enforces the
 * Interface Segregation Principle (ISP) in OOP.
 */
public interface InventoryManageable {

    /** Adds a new vehicle to the inventory. */
    void addVehicle(Vehicle vehicle);

    /** Removes a vehicle from inventory by ID if not currently rented. */
    boolean removeVehicle(String vehicleId);

    /** Updates existing vehicle attributes. */
    boolean updateVehicle(String vehicleId, String newBrand, String newModel, double newRate);

    /** Returns all vehicles currently managed in the inventory. */
    List<Vehicle> getAllVehicles();

    /** Finds a vehicle by its unique ID, or returns null if not found. */
    Vehicle findVehicleById(String vehicleId);
}
