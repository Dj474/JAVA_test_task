package com.pingine.fleetpulse.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TripsForVehicleNotFoundException extends ResponseStatusException {

    public TripsForVehicleNotFoundException(String vehicleId) {
        super(HttpStatus.NOT_FOUND, "trips for vehicle " + vehicleId + "not found");
    }
}
