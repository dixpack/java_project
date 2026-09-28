package com.rental.model;

import java.time.LocalDate;

/**
 * Represents a single rental transaction linking a Customer to a Vehicle
 * for a given date range, along with the financial details of that
 * transaction (cost, deposit, and any late-return penalty).
 *
 * LocalDate is used instead of raw Strings for dates so duration
 * calculations (days rented, days late) are accurate and simple.
 */
public class Rental {

    private String rentalId;
    private Customer customer;
    private Vehicle vehicle;
    private LocalDate startDate;
    private LocalDate endDate;      // scheduled/expected return date
    private double rentalCost;
    private double securityDeposit;
    private double lateReturnPenalty; // 0 unless the vehicle is returned late
    private boolean active;           // true while the vehicle is still out with the customer
    private LocalDate actualReturnDate; // recorded upon return
    private double refundAmount;      // security deposit refund recorded upon return

    public Rental(String rentalId, Customer customer, Vehicle vehicle,
                   LocalDate startDate, LocalDate endDate,
                   double rentalCost, double securityDeposit) {
        this.rentalId = rentalId;
        this.customer = customer;
        this.vehicle = vehicle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rentalCost = rentalCost;
        this.securityDeposit = securityDeposit;
        this.lateReturnPenalty = 0.0;
        this.active = true;
        this.actualReturnDate = null;
        this.refundAmount = 0.0;
    }

    public String getRentalId() {
        return rentalId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getRentalCost() {
        return rentalCost;
    }

    public double getSecurityDeposit() {
        return securityDeposit;
    }

    public double getLateReturnPenalty() {
        return lateReturnPenalty;
    }

    public void setLateReturnPenalty(double lateReturnPenalty) {
        this.lateReturnPenalty = lateReturnPenalty;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(LocalDate actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    @Override
    public String toString() {
        return String.format(
                "Rental[ID=%s, Customer=%s, Vehicle=%s, Start=%s, End=%s, Cost=%.2f, Deposit=%.2f, Penalty=%.2f, Refund=%.2f, Active=%b]",
                rentalId, customer.getName(), vehicle.getVehicleId(), startDate, endDate,
                rentalCost, securityDeposit, lateReturnPenalty, refundAmount, active);
    }
}
