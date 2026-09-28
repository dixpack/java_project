package com.rental.model;

/**
 * Plain data class representing a rentable vehicle.
 * Deliberately holds no business logic - that responsibility belongs to
 * RentalAdmin. Vehicle only knows about its own state.
 */
public class Vehicle {

    private String vehicleId;
    private VehicleType type;
    private String brand;
    private String model;
    private double rentalRate;   // cost per day
    private AvailabilityStatus availabilityStatus;

    public Vehicle(String vehicleId, VehicleType type, String brand, String model, double rentalRate) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.rentalRate = rentalRate;
        this.availabilityStatus = AvailabilityStatus.AVAILABLE; // new vehicles start available
    }

    // ---- Getters ----
    public String getVehicleId() {
        return vehicleId;
    }

    public VehicleType getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public double getRentalRate() {
        return rentalRate;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    // ---- Setters (used by RentalAdmin for updates and status changes) ----
    public void setType(VehicleType type) {
        this.type = type;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setRentalRate(double rentalRate) {
        this.rentalRate = rentalRate;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    /**
     * Polymorphic method to be overridden by specialized vehicle subclasses.
     */
    public String getSpecificDetails() {
        return String.format("%s %s", brand, model);
    }

    @Override
    public String toString() {
        return String.format("Vehicle[ID=%s, Type=%s, Brand=%s, Model=%s, Rate=%.2f/day, Status=%s]",
                vehicleId, type, brand, model, rentalRate, availabilityStatus);
    }
}
