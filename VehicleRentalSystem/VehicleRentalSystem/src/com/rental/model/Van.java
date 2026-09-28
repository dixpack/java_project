package com.rental.model;

/**
 * Specialized Vehicle subclass representing a commercial / passenger van.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Van extends Vehicle {

    private double cargoCapacityKg;
    private boolean hasSlidingDoor;

    public Van(String vehicleId, String brand, String model, double rentalRate,
               double cargoCapacityKg, boolean hasSlidingDoor) {
        super(vehicleId, VehicleType.VAN, brand, model, rentalRate);
        this.cargoCapacityKg = cargoCapacityKg;
        this.hasSlidingDoor = hasSlidingDoor;
    }

    public Van(String vehicleId, String brand, String model, double rentalRate) {
        this(vehicleId, brand, model, rentalRate, 1000.0, true);
    }

    public double getCargoCapacityKg() {
        return cargoCapacityKg;
    }

    public void setCargoCapacityKg(double cargoCapacityKg) {
        this.cargoCapacityKg = cargoCapacityKg;
    }

    public boolean isHasSlidingDoor() {
        return hasSlidingDoor;
    }

    public void setHasSlidingDoor(boolean hasSlidingDoor) {
        this.hasSlidingDoor = hasSlidingDoor;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Cargo: %.0f kg, %s", cargoCapacityKg, hasSlidingDoor ? "Sliding Door" : "Standard Door");
    }
}
