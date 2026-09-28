package com.rental.model;

/**
 * Represents the real-time availability state of a vehicle.
 * This is flipped immediately by RentalAdmin whenever a booking or a
 * return occurs, so the system always reflects the true current state
 * without needing a separate "refresh" step.
 */
public enum AvailabilityStatus {
    AVAILABLE,
    RENTED,
    MAINTENANCE
}
