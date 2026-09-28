package com.rental.test;

import com.rental.model.*;
import com.rental.service.RentalAdmin;

import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive automated test suite for the Vehicle Rental System.
 * Tests all OOP principles, interface behaviors, real-time availability tracking,
 * booking logic, late-return penalties, financial caps, boundary conditions, and edge cases.
 */
public class RentalSystemTest {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("     RUNNING VEHICLE RENTAL SYSTEM TEST SUITE    ");
        System.out.println("=================================================");

        // TC-01: OOP Polymorphism & Dynamic Dispatch
        testVehiclePolymorphism();

        // TC-02, TC-03, TC-04: Inventory Management & Searchable Interface
        testInventoryAddAndSearch();

        // TC-05: Inventory Update
        testInventoryUpdate();

        // TC-06, TC-07: Booking & Real-Time Availability
        testSuccessfulBookingAndRealTimeAvailability();

        // TC-08: Concurrency / Double Booking Prevention
        testPreventDoubleBooking();

        // TC-09: Active Lease Deletion Protection
        testPreventRemovingRentedVehicle();

        // TC-10: On-Time Return & Full Refund
        testReturnOnTimeWithFullRefund();

        // TC-11: Late Return Penalty Assessment
        testReturnLateWithPenaltyCalculation();

        // TC-12: Deposit Depletion Cap (No Negative Refund)
        testDepositDepletionCap();

        // TC-13: Customer Rental History Tracking
        testCustomerRentalHistory();

        // TC-14: Boundary & Invalid Date Range Protection
        testInvalidDateRangeProtection();

        // TC-15: Non-Existent Vehicle Operations Graceful Handling
        testNonExistentVehicleOperations();

        // TC-16: Duplicate Primary Key ID Protection
        testDuplicateIdProtection();

        // TC-17: Duplicate Return Operation Prevention
        testDoubleReturnProtection();

        System.out.println("\n-------------------------------------------------");
        System.out.printf("TEST RESULTS: %d PASSED, %d FAILED%n", passedCount, failedCount);
        System.out.println("=================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String errorDetail) {
        if (condition) {
            System.out.println(" [PASS] " + testName);
            passedCount++;
        } else {
            System.err.println(" [FAIL] " + testName + " - " + errorDetail);
            failedCount++;
        }
    }

    // =========================================================
    // TEST IMPLEMENTATIONS
    // =========================================================

    /** TC-01: Polymorphism and method overriding */
    private static void testVehiclePolymorphism() {
        Vehicle car = new Car("T_CAR1", "BMW", "M3", 180.0, 4, "Petrol");
        Vehicle bike = new Bike("T_BIK1", "Kawasaki", "Ninja 400", 75.0, 399, true);
        Vehicle van = new Van("T_VAN1", "Mercedes", "Sprinter", 130.0, 1500.0, true);

        assertTrue("Polymorphic Subclass Types",
                car.getType() == VehicleType.CAR && bike.getType() == VehicleType.BIKE && van.getType() == VehicleType.VAN,
                "Vehicle types do not match expected enums");

        assertTrue("Polymorphic Method Overriding",
                car.getSpecificDetails().contains("Seats") &&
                bike.getSpecificDetails().contains("cc") &&
                van.getSpecificDetails().contains("Cargo"),
                "getSpecificDetails() did not return subclass-specific string");
    }

    /** TC-02, TC-03, TC-04: Inventory Add and Searchable Interface */
    private static void testInventoryAddAndSearch() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle v1 = new Car("V101", "Tesla", "Model 3", 120.0);
        Vehicle v2 = new Car("V102", "Tesla", "Model Y", 140.0);
        Vehicle v3 = new Bike("V103", "Yamaha", "R3", 60.0);

        admin.addVehicle(v1);
        admin.addVehicle(v2);
        admin.addVehicle(v3);

        List<Vehicle> teslas = admin.searchByBrand("Tesla");
        assertTrue("Search By Brand", teslas.size() == 2, "Expected 2 Tesla vehicles, found " + teslas.size());

        List<Vehicle> bikes = admin.searchByType(VehicleType.BIKE);
        assertTrue("Search By Type", bikes.size() == 1 && bikes.get(0).getVehicleId().equals("V103"),
                "Expected 1 bike with ID V103");
    }

    /** TC-05: Inventory Update */
    private static void testInventoryUpdate() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle v = new Car("V201", "Honda", "Civic", 80.0);
        admin.addVehicle(v);

        boolean updated = admin.updateVehicle("V201", "Honda", "Accord", 95.0);
        Vehicle fetched = admin.findVehicleById("V201");

        assertTrue("Vehicle Update Operation",
                updated && fetched.getModel().equals("Accord") && fetched.getRentalRate() == 95.0,
                "Vehicle update did not reflect new values");
    }

    /** TC-06, TC-07: Booking Cost & Real-Time Availability Flip */
    private static void testSuccessfulBookingAndRealTimeAvailability() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V301", "Audi", "RS6", 250.0);
        Customer cust = new Customer("C301", "Vikram Rathore", "vikram@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        assertTrue("Initial Status is AVAILABLE",
                car.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE,
                "Expected AVAILABLE initially");

        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 5); // 4 days * 250 = 1000
        Rental rental = admin.bookVehicle(cust, car, start, end, 300.0);

        assertTrue("Booking Created Successfully", rental != null, "Rental should not be null");
        assertTrue("Rental Cost Calculation (4 days * $250 = $1000)",
                rental.getRentalCost() == 1000.0, "Cost was " + rental.getRentalCost());
        assertTrue("Security Deposit Recorded",
                rental.getSecurityDeposit() == 300.0, "Deposit was " + rental.getSecurityDeposit());

        assertTrue("Real-Time Availability Flip to RENTED",
                car.getAvailabilityStatus() == AvailabilityStatus.RENTED,
                "Vehicle status must immediately become RENTED");
    }

    /** TC-08: Concurrency / Double Booking Prevention */
    private static void testPreventDoubleBooking() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V401", "Hyundai", "Ioniq 5", 110.0);
        Customer cust1 = new Customer("C401", "Alice", "alice@example.com");
        Customer cust2 = new Customer("C402", "Bob", "bob@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust1);
        admin.addCustomer(cust2);

        Rental r1 = admin.bookVehicle(cust1, car, LocalDate.now(), LocalDate.now().plusDays(2), 200.0);
        Rental r2 = admin.bookVehicle(cust2, car, LocalDate.now(), LocalDate.now().plusDays(3), 200.0);

        assertTrue("Primary Booking Accepted", r1 != null, "First booking should succeed");
        assertTrue("Double Booking Rejected for RENTED Vehicle", r2 == null,
                "Second booking must be rejected because vehicle is already rented");
    }

    /** TC-09: Active Lease Deletion Protection */
    private static void testPreventRemovingRentedVehicle() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V501", "Kia", "EV6", 130.0);
        Customer cust = new Customer("C501", "Charlie", "charlie@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        admin.bookVehicle(cust, car, LocalDate.now(), LocalDate.now().plusDays(2), 200.0);
        boolean removed = admin.removeVehicle("V501");

        assertTrue("Prevent Removing Active Leased Vehicle", !removed,
                "System must refuse removing a vehicle that is currently RENTED");
    }

    /** TC-10: Return On-Time & Full Refund */
    private static void testReturnOnTimeWithFullRefund() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V601", "Toyota", "Camry", 90.0);
        Customer cust = new Customer("C601", "David", "david@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        LocalDate start = LocalDate.now().minusDays(3);
        LocalDate scheduledEnd = LocalDate.now();
        Rental rental = admin.bookVehicle(cust, car, start, scheduledEnd, 400.0);

        double refund = admin.returnVehicle(rental.getRentalId(), scheduledEnd);

        assertTrue("On-time Full Deposit Refund", refund == 400.0,
                "Expected full refund of $400, got: " + refund);
        assertTrue("Zero Late Penalty on On-time Return", rental.getLateReturnPenalty() == 0.0,
                "Penalty should be 0.0");
        assertTrue("Real-Time Availability Flip back to AVAILABLE",
                car.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE,
                "Vehicle status must return to AVAILABLE");
        assertTrue("Rental Inactive After Return", !rental.isActive(), "Rental active flag must be false");
    }

    /** TC-11: Late Return Penalty Assessment */
    private static void testReturnLateWithPenaltyCalculation() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V701", "Mercedes", "C-Class", 350.0);
        Customer cust = new Customer("C701", "Emma", "emma@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate scheduledEnd = LocalDate.of(2026, 5, 4);
        LocalDate actualReturn = LocalDate.of(2026, 5, 6); // 2 days late!
        // 2 days * $500 = $1000 penalty. Deposit is $1500, refund = $1500 - $1000 = $500.

        Rental rental = admin.bookVehicle(cust, car, start, scheduledEnd, 1500.0);
        double refund = admin.returnVehicle(rental.getRentalId(), actualReturn);

        assertTrue("Late Penalty Applied ($1000 for 2 days)", rental.getLateReturnPenalty() == 1000.0,
                "Expected penalty $1000, got: " + rental.getLateReturnPenalty());
        assertTrue("Deposit Refund After Penalty ($1500 - $1000 = $500)", refund == 500.0,
                "Expected refund $500, got: " + refund);
        assertTrue("Real-Time Availability Flip back to AVAILABLE after Late Return",
                car.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE,
                "Vehicle status must return to AVAILABLE");
    }

    /** TC-12: Deposit Depletion Cap (Penalty > Deposit) */
    private static void testDepositDepletionCap() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V_CAP1", "BMW", "330i", 200.0);
        Customer cust = new Customer("C_CAP1", "Grace Hopper", "grace@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate scheduledEnd = LocalDate.of(2026, 6, 3);
        LocalDate actualReturn = LocalDate.of(2026, 6, 6); // 3 days late @ $500 = $1500 penalty!

        Rental rental = admin.bookVehicle(cust, car, start, scheduledEnd, 500.0);
        double refund = admin.returnVehicle(rental.getRentalId(), actualReturn);

        assertTrue("Penalty Exceeds Deposit ($1500 vs $500)", rental.getLateReturnPenalty() == 1500.0,
                "Expected penalty $1500, got: " + rental.getLateReturnPenalty());
        assertTrue("Refund Floored at Zero (Deposit Depletion Cap)", refund == 0.0,
                "Refund must be 0.0 when penalty exceeds deposit, got: " + refund);
    }

    /** TC-13: Customer Rental History Retention */
    private static void testCustomerRentalHistory() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car1 = new Car("V801", "Nissan", "GT-R", 300.0);
        Vehicle car2 = new Car("V802", "Subaru", "WRX", 100.0);
        Customer cust = new Customer("C801", "Franklin", "franklin@example.com");
        admin.addVehicle(car1);
        admin.addVehicle(car2);
        admin.addCustomer(cust);

        Rental r1 = admin.bookVehicle(cust, car1, LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), 400.0);
        admin.returnVehicle(r1.getRentalId(), LocalDate.now().minusDays(2));

        Rental r2 = admin.bookVehicle(cust, car2, LocalDate.now(), LocalDate.now().plusDays(2), 200.0);

        assertTrue("Customer Rental History Contains Both Leases",
                cust.getRentalHistory().size() == 2,
                "Expected 2 rentals in customer history, found " + cust.getRentalHistory().size());
    }

    /** TC-14: Boundary & Invalid Date Range Protection */
    private static void testInvalidDateRangeProtection() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V_DATE1", "Tesla", "Model 3", 150.0);
        Customer cust = new Customer("C_DATE1", "Alan Turing", "alan@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        LocalDate start = LocalDate.of(2026, 7, 10);
        LocalDate endSame = LocalDate.of(2026, 7, 10);
        LocalDate endBefore = LocalDate.of(2026, 7, 9);

        Rental r1 = admin.bookVehicle(cust, car, start, endSame, 500.0);
        Rental r2 = admin.bookVehicle(cust, car, start, endBefore, 500.0);

        assertTrue("Reject Same-Day Booking (0 Duration)", r1 == null, "Same-day booking should be rejected");
        assertTrue("Reject Inverted Date Range (End Before Start)", r2 == null, "Inverted date range should be rejected");
        assertTrue("Vehicle Retains AVAILABLE Status After Rejected Booking",
                car.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE,
                "Vehicle must remain AVAILABLE");
    }

    /** TC-15: Non-Existent Vehicle Operations Graceful Handling */
    private static void testNonExistentVehicleOperations() {
        RentalAdmin admin = new RentalAdmin();
        boolean removed = admin.removeVehicle("NON_EXISTENT_ID");
        boolean updated = admin.updateVehicle("NON_EXISTENT_ID", "Brand", "Model", 100.0);
        Vehicle found = admin.findVehicleById("NON_EXISTENT_ID");

        assertTrue("Remove Non-Existent Vehicle Returns False", !removed, "Should return false");
        assertTrue("Update Non-Existent Vehicle Returns False", !updated, "Should return false");
        assertTrue("Find Non-Existent Vehicle Returns Null", found == null, "Should return null");
    }

    /** TC-16: Duplicate Primary Key ID Protection */
    private static void testDuplicateIdProtection() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle v1 = new Car("DUP_V1", "Ford", "Mustang", 300.0);
        Vehicle v2 = new Car("DUP_V1", "Chevrolet", "Camaro", 320.0);
        admin.addVehicle(v1);
        admin.addVehicle(v2);
        assertTrue("Prevent Duplicate Vehicle ID Registration",
                admin.getAllVehicles().size() == 1,
                "Inventory should have only 1 vehicle");

        Customer c1 = new Customer("DUP_C1", "John Doe", "john@example.com");
        Customer c2 = new Customer("DUP_C1", "Jane Doe", "jane@example.com");
        admin.addCustomer(c1);
        admin.addCustomer(c2);
        assertTrue("Prevent Duplicate Customer ID Registration",
                admin.getAllCustomers().size() == 1,
                "Customer directory should have only 1 customer");
    }

    /** TC-17: Duplicate Return Operation Prevention */
    private static void testDoubleReturnProtection() {
        RentalAdmin admin = new RentalAdmin();
        Vehicle car = new Car("V_RET1", "Audi", "A4", 180.0);
        Customer cust = new Customer("C_RET1", "Ada Lovelace", "ada@example.com");
        admin.addVehicle(car);
        admin.addCustomer(cust);

        Rental rental = admin.bookVehicle(cust, car, LocalDate.now().minusDays(2), LocalDate.now(), 500.0);
        double refund1 = admin.returnVehicle(rental.getRentalId(), LocalDate.now());
        double refund2 = admin.returnVehicle(rental.getRentalId(), LocalDate.now());

        assertTrue("First Return Succeeds with Full Refund", refund1 == 500.0,
                "First return refund should be 500");
        assertTrue("Duplicate Return Rejected with -1 Code", refund2 == -1.0,
                "Duplicate return should return -1");
    }
}
