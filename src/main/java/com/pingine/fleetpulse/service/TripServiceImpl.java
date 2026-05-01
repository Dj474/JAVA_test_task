package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Override
    public TripResponse getLastTrip(String vehicleId) {

        VehicleResponse vehicle = vehicleService.getById(vehicleId);

        List<TelemetryPoint> points = telemetryRepository.findByVehicleId(vehicleId);

        if (points == null || points.isEmpty()) {
            throw new TripsForVehicleNotFoundException(vehicleId);
        }

        List<Trip> trips = tripDetector.detect(points);
        Trip lastTrip = trips.get(trips.size() - 1);

        return TripResponse.builder()
                .vehicle(vehicle)
                .startedAt(lastTrip.getStartedAt())
                .endedAt(lastTrip.getEndedAt())
                .distanceKm(lastTrip.getDistanceKm())
                .avgSpeedKph(lastTrip.getAvgSpeedKph())
                .pointCount(lastTrip.getPoints().size())
                .points(mapPointsToDtos(lastTrip.getPoints())).build();

    }

    private List<TripResponse.PointDto> mapPointsToDtos(List<Trip.TripPoint> points) {
        List<TripResponse.PointDto> dtos = new ArrayList<>();
        for (Trip.TripPoint point : points) {
            dtos.add(TripResponse.PointDto.builder()
                    .ts(point.getTs())
                    .lat(point.getLat())
                    .lon(point.getLon())
                    .speedKph(point.getSpeedKph())
                    .build()
            );
        }
        return dtos;
    }

}
