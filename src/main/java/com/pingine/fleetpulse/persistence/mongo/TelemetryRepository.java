package com.pingine.fleetpulse.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends MongoRepository<TelemetryPoint, String>, TripQueryRepository {

    List<TelemetryPoint> findByVehicleId(String vehicleId);

}
