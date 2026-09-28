package com.rental.interfaces;

import com.rental.model.Customer;
import com.rental.model.Rental;
import com.rental.model.Vehicle;
import java.time.LocalDate;

/**
 * Contract for anything capable of processing bookings and returns.
 * Declaring this as an interface (rather than putting the logic straight
 * into RentalAdmin) separates "what the system can do" from "how it does
 * it", which is the core OOP requirement of this project.
 */
public interface Rentable {

    /**
     * Books a vehicle for a customer over the given date range, collects
     * the security deposit, and calculates the rental cost.
     * Returns the created Rental record, or null if the vehicle is not
     * currently available.
     */
    Rental bookVehicle(Customer customer, Vehicle vehicle, LocalDate startDate,
                        LocalDate endDate, double securityDeposit);

    /**
     * Processes the return of a previously booked rental, applying a
     * late-return penalty if applicable, and returns the refund amount
     * owed to the customer (deposit minus any penalty, never negative).
     */
    double returnVehicle(String rentalId, LocalDate actualReturnDate);
}
