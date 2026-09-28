package com.rental.model;

/**
 * Specialized Vehicle subclass representing a two-wheeler / motorcycle.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Bike extends Vehicle {

    private int engineCapacityCc;
    private boolean helmetIncluded;

    public Bike(String vehicleId, String brand, String model, double rentalRate,
                int engineCapacityCc, boolean helmetIncluded) {
        super(vehicleId, VehicleType.BIKE, brand, model, rentalRate);
        this.engineCapacityCc = engineCapacityCc;
        this.helmetIncluded = helmetIncluded;
    }

    public Bike(String vehicleId, String brand, String model, double rentalRate) {
        this(vehicleId, brand, model, rentalRate, 250, true);
    }

    public int getEngineCapacityCc() {
        return engineCapacityCc;
    }

    public void setEngineCapacityCc(int engineCapacityCc) {
        this.engineCapacityCc = engineCapacityCc;
    }

    public boolean isHelmetIncluded() {
        return helmetIncluded;
    }

    public void setHelmetIncluded(boolean helmetIncluded) {
        this.helmetIncluded = helmetIncluded;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("%d cc, Helmet %s", engineCapacityCc, helmetIncluded ? "Included" : "Not Provided");
    }
}
