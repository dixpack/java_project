package com.rental.model;

/**
 * Specialized Vehicle subclass representing an automobile.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Car extends Vehicle {

    private int seatingCapacity;
    private String fuelType; // Petrol, Diesel, Electric, Hybrid

    public Car(String vehicleId, String brand, String model, double rentalRate,
               int seatingCapacity, String fuelType) {
        super(vehicleId, VehicleType.CAR, brand, model, rentalRate);
        this.seatingCapacity = seatingCapacity;
        this.fuelType = fuelType;
    }

    public Car(String vehicleId, String brand, String model, double rentalRate) {
        this(vehicleId, brand, model, rentalRate, 5, "Petrol");
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("%d Seats, %s", seatingCapacity, fuelType);
    }
}
