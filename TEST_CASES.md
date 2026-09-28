# Vehicle Rental System — Test Cases & Quality Assurance Matrix

## 1. Test Suite Summary
The system has been evaluated against comprehensive unit, integration, boundary, and scenario-based test cases covering:
- **OOP Polymorphic Subclasses & Encapsulation**
- **Interface Realization (`Rentable`, `Searchable`, `InventoryManageable`)**
- **Real-Time State Transitions & Concurrency Guards**
- **Financial Calculations (Duration Costs, Deposits, Late Penalties, Net Refunds)**
- **Deposit Depletion Cap & Floor Guard (Zero Negative Refunds)**
- **Customer Audit History & Data Integrity**
- **Boundary Conditions (Same-Day / Inverted Dates, Duplicate IDs, Closed Lease Guards)**

All 33 automated assertions in `com.rental.test.RentalSystemTest` execute and pass with **100% success rate (0 failures)**.

---

## 2. Test Execution Matrix

| Test ID | Module / Feature | Test Objective & Scenario | Preconditions | Input Data | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-01** | OOP Polymorphism | Verify specialized subclass instantiation and polymorphic method overriding for `Car`, `Bike`, and `Van`. | Classes compiled. | `new Car("T1", "BMW", "M3", 180, 4, "Petrol")`, `new Bike("T2", "Kawasaki", "Ninja", 75, 399, true)`, `new Van("T3", "Mercedes", "Sprinter", 130, 1500, true)` | `Car.getType() == CAR`, `Bike.getType() == BIKE`, `Van.getType() == VAN`. Subclasses return distinct `getSpecificDetails()`. | Subclasses inherit and correctly format type-specific features. | **PASS** |
| **TC-02** | Inventory Management | Add new vehicles to the inventory collection. | Clean inventory. | `Vehicle ID: V101`, `Brand: Tesla`, `Model: Model 3`, `Rate: 120.0` | Vehicle is stored in inventory; `findVehicleById("V101")` returns vehicle with `status = AVAILABLE`. | Vehicle successfully added to list. | **PASS** |
| **TC-03** | Searchable Interface | Search available vehicles filtered by `VehicleType`. | 2 Cars and 1 Bike registered and `AVAILABLE`. | `searchByType(VehicleType.BIKE)` | Exactly 1 vehicle returned with ID matching the bike (`V103`). | Returned 1 matching bike. | **PASS** |
| **TC-04** | Searchable Interface | Search available vehicles by brand name (case-insensitive substring). | 2 Tesla vehicles and 1 Yamaha in inventory. | `searchByBrand("tesla")` | Returns both Tesla models (`V101`, `V102`). | 2 matching vehicles returned. | **PASS** |
| **TC-05** | Inventory Update | Update brand, model, and daily rate of an existing vehicle. | Vehicle `V201` exists with model "Civic" and rate 80.0. | `updateVehicle("V201", "Honda", "Accord", 95.0)` | `findVehicleById("V201")` reflects Model="Accord" and Rate=95.0. | Attributes updated properly. | **PASS** |
| **TC-06** | Booking Calculation | Calculate rental cost based on day count and record security deposit. | Vehicle `V301` rate = 250.0/day. Customer `C301` registered. | `Start: 2026-10-01`, `End: 2026-10-05` (4 days), `Deposit: 300.0` | $\text{Cost} = 4 \times 250 = ₹1000.0$. Rental record created with Deposit = ₹300.0. | Rental record created with Cost=1000.0, Deposit=300.0. | **PASS** |
| **TC-07** | Real-Time Availability | Verify vehicle status flips immediately from `AVAILABLE` to `RENTED` upon booking. | Vehicle `V301` status is `AVAILABLE`. | Booking confirmed for `V301`. | `vehicle.getAvailabilityStatus() == RENTED`. | Status changed to `RENTED` immediately. | **PASS** |
| **TC-08** | Concurrency / Double Booking | Prevent booking an already rented vehicle. | Vehicle `V401` has active booking with Customer `C401`. | Customer `C402` attempts to book `V401` for overlapping dates. | Booking is rejected (`null` returned), error logged: "Vehicle is currently RENTED". | Double-booking blocked; returns `null`. | **PASS** |
| **TC-09** | Deletion Protection | Prevent removal of a vehicle currently rented out. | Vehicle `V501` has active lease. | `removeVehicle("V501")` | Method returns `false`. Vehicle remains in inventory. | Removal blocked with error message. | **PASS** |
| **TC-10** | Return On-Time | Process return on or before scheduled end date with zero penalty. | Rental `R001` scheduled end: `2026-09-24`, Deposit: ₹400.0. | `Actual Return: 2026-09-24` | $\text{Late Penalty} = ₹0.00$. Full refund of ₹400.00 issued. Vehicle status restores to `AVAILABLE`. | Penalty=0.0, Refund=400.0, Status=AVAILABLE. | **PASS** |
| **TC-11** | Late Return Penalty | Return vehicle 2 days late and assess daily late fee fine. | Rental scheduled end: `2026-05-04`, Deposit: ₹1500.0, Late fee = ₹500/day. | `Actual Return: 2026-05-06` (2 days late) | $\text{Penalty} = 2 \times 500 = ₹1000.0$. $\text{Refund} = 1500 - 1000 = ₹500.0$. Status restores to `AVAILABLE`. | Penalty=1000.0, Refund=500.0, Status=AVAILABLE. | **PASS** |
| **TC-12** | Deposit Depletion Cap | Verify refund cannot be negative if late penalty exceeds deposit. | Deposit = ₹500.0. Vehicle returned 3 days late. Late fee = ₹500/day. | $\text{Penalty} = 3 \times 500 = ₹1500.0$. | $\text{Refund} = \max(0, 500 - 1500) = ₹0.00$. Refund floored at zero. | Penalty=1500.0, Refund=0.0. | **PASS** |
| **TC-13** | Customer History | Verify that every completed or active booking is appended to customer's history. | Customer `C801` with 0 prior rentals. | Book vehicle 1, return it, then book vehicle 2. | `customer.getRentalHistory().size() == 2`. | History retains both past and active lease records. | **PASS** |
| **TC-14** | Invalid Date Range Protection | Reject bookings where end date $\le$ start date. | Vehicle `V_DATE1` is `AVAILABLE`. | `start = 2026-07-10`, `end = 2026-07-10` and `end = 2026-07-09` | Booking rejected (`null`); vehicle remains `AVAILABLE`. | Both rejected; vehicle remains `AVAILABLE`. | **PASS** |
| **TC-15** | Non-Existent ID Handling | Safe handling of lookups, updates, and deletions for missing IDs. | Empty inventory. | Operations on ID `NON_EXISTENT_ID`. | Return `false` / `null` without throwing `NullPointerException`. | Handled gracefully without crash. | **PASS** |
| **TC-16** | Duplicate Primary Key Guard | Prevent adding duplicate vehicle or customer IDs. | Vehicle `DUP_V1` and Customer `DUP_C1` exist. | Attempt adding second entity with same ID. | Rejection logged; duplicate not appended to list. | Duplicates blocked; list size remains 1. | **PASS** |
| **TC-17** | Duplicate Return Guard | Prevent returning an already returned/closed lease. | Rental `R001` already closed. | `returnVehicle(R001)` invoked a second time. | Method returns `-1.0` with error message; no double refund. | Double return blocked; returns `-1.0`. | **PASS** |

---

## 3. How to Run the Automated Test Suite

Run the automated test runner in PowerShell or terminal:

```powershell
cd "c:\Users\psuth\Desktop\works\java project\VehicleRentalSystem\VehicleRentalSystem"
javac -d bin -sourcepath src (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -cp bin com.rental.test.RentalSystemTest
```

### Verified Terminal Output:
```
=================================================
     RUNNING VEHICLE RENTAL SYSTEM TEST SUITE    
=================================================
 [PASS] Polymorphic Subclass Types
 [PASS] Polymorphic Method Overriding
 [PASS] Search By Brand
 [PASS] Search By Type
 [PASS] Vehicle Update Operation
 [PASS] Initial Status is AVAILABLE
 [PASS] Booking Created Successfully
 [PASS] Rental Cost Calculation (4 days * $250 = $1000)
 [PASS] Security Deposit Recorded
 [PASS] Real-Time Availability Flip to RENTED
 [PASS] Primary Booking Accepted
 [PASS] Double Booking Rejected for RENTED Vehicle
 [PASS] Prevent Removing Active Leased Vehicle
 [PASS] On-time Full Deposit Refund
 [PASS] Zero Late Penalty on On-time Return
 [PASS] Real-Time Availability Flip back to AVAILABLE
 [PASS] Rental Inactive After Return
 [PASS] Late Penalty Applied ($1000 for 2 days)
 [PASS] Deposit Refund After Penalty ($1500 - $1000 = $500)
 [PASS] Real-Time Availability Flip back to AVAILABLE after Late Return
 [PASS] Penalty Exceeds Deposit ($1500 vs $500)
 [PASS] Refund Floored at Zero (Deposit Depletion Cap)
 [PASS] Customer Rental History Contains Both Leases
 [PASS] Reject Same-Day Booking (0 Duration)
 [PASS] Reject Inverted Date Range (End Before Start)
 [PASS] Vehicle Retains AVAILABLE Status After Rejected Booking
 [PASS] Remove Non-Existent Vehicle Returns False
 [PASS] Update Non-Existent Vehicle Returns False
 [PASS] Find Non-Existent Vehicle Returns Null
 [PASS] Prevent Duplicate Vehicle ID Registration
 [PASS] Prevent Duplicate Customer ID Registration
 [PASS] First Return Succeeds with Full Refund
 [PASS] Duplicate Return Rejected with -1 Code

-------------------------------------------------
TEST RESULTS: 33 PASSED, 0 FAILED
=================================================
```
