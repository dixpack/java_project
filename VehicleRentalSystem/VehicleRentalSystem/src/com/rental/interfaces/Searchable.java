package com.rental.interfaces;

import com.rental.model.Vehicle;
import com.rental.model.VehicleType;
import java.util.List;

/**
 * Contract for anything capable of searching the vehicle inventory.
 */
public interface Searchable {

    /** Returns all currently available vehicles matching the given type. */
    List<Vehicle> searchByType(VehicleType type);

    /** Returns all currently available vehicles matching the given brand
     *  (case-insensitive). */
    List<Vehicle> searchByBrand(String brand);
}
