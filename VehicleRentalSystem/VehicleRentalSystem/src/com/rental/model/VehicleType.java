package com.rental.model;

/**
 * Represents the category of a vehicle.
 * Using an enum instead of a raw String prevents typos (e.g. "cra" instead
 * of "car") and gives compiler-checked, IDE-autocompleted values.
 */
public enum VehicleType {
    CAR,
    BIKE,
    VAN
}
