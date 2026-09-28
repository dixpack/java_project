package com.rental.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer of the rental system.
 * Keeps a running list of every Rental (past and present) associated
 * with this customer, so rental history can be retrieved on demand.
 */
public class Customer {

    private String customerId;
    private String name;
    private String contactDetails;
    private String licenceNumber;
    private List<Rental> rentalHistory;

    public Customer(String customerId, String name, String contactDetails) {
        this(customerId, name, contactDetails, "DL-" + customerId + "9876");
    }

    public Customer(String customerId, String name, String contactDetails, String licenceNumber) {
        this.customerId = customerId;
        this.name = name;
        this.contactDetails = contactDetails;
        this.licenceNumber = licenceNumber != null && !licenceNumber.trim().isEmpty() ? licenceNumber.trim() : "DL-" + customerId + "9876";
        this.rentalHistory = new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public String getLicenceNumber() {
        return licenceNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public void setLicenceNumber(String licenceNumber) {
        this.licenceNumber = licenceNumber;
    }

    public List<Rental> getRentalHistory() {
        return rentalHistory;
    }

    /**
     * Called by RentalAdmin whenever a new Rental is created for this
     * customer, so the customer's history always stays up to date.
     */
    public void addToHistory(Rental rental) {
        this.rentalHistory.add(rental);
    }

    @Override
    public String toString() {
        return String.format("Customer[ID=%s, Name=%s, Contact=%s, Licence=%s, TotalRentals=%d]",
                customerId, name, contactDetails, licenceNumber, rentalHistory.size());
    }
}
