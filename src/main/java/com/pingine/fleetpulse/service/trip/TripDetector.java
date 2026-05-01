package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
public class TripDetector {

    public List<Trip> detect(List<TelemetryPoint> points) {
        Map<String, List<TelemetryPoint>> pointsByVehicle = new HashMap<>();
        for (TelemetryPoint point: points) {
            String curr = point.getVehicleId();
            if (!pointsByVehicle.containsKey(curr)) {
                pointsByVehicle.put(curr, new ArrayList<>());
            }
            pointsByVehicle.get(curr).add(point);
        }
        List<Trip> trips = new ArrayList<>();
        for (List<TelemetryPoint> vehiclePoints : pointsByVehicle.values()) {
            vehiclePoints.sort(Comparator.comparing(TelemetryPoint::getTs));

            trips.addAll(getTripsForOneVehicle(vehiclePoints));
        }
        return trips;

    }

    private List<Trip> getTripsForOneVehicle(List<TelemetryPoint> points) {
        List<Trip> trips = new ArrayList<>();
        List<TelemetryPoint> currentPoints = new ArrayList<>();
        Set<LocalDateTime> seenTimestamps = new HashSet<>();
        for (TelemetryPoint point : points) {
            if (seenTimestamps.contains(point.getTs())) {
                continue;
            }
            seenTimestamps.add(point.getTs());

            currentPoints.add(point);
            if (!point.isIgnition()) {
                trips.add(getTripFromPoints(currentPoints));
                currentPoints.clear();
                seenTimestamps.clear();
            }
        }
        return trips;
    }

    private Trip getTripFromPoints(List<TelemetryPoint> points) {
        List<Trip.TripPoint> tripPoints = points.stream()
                .map(p -> Trip.TripPoint.builder()
                        .ts(p.getTs().toInstant(ZoneOffset.UTC))
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .speedKph(p.getSpeed())
                        .build())
                .collect(Collectors.toList());

        double distanceKm = calculateDistance(points);

        Instant startedAt = tripPoints.get(0).getTs();
        Instant endedAt = tripPoints.get(tripPoints.size() - 1).getTs();

        Duration duration = Duration.between(startedAt, endedAt);

        double avgSpeedKph = distanceKm / (duration.toMillis() / 3600000.0);

        return Trip.builder()
                .vehicleId(points.get(0).getVehicleId())
                .startedAt(tripPoints.get(0).getTs())
                .endedAt(tripPoints.get(tripPoints.size() - 1).getTs())
                .distanceKm(distanceKm)
                .avgSpeedKph(avgSpeedKph)
                .points(tripPoints)
                .build();

    }

    private double calculateDistance(List<TelemetryPoint> points) {
        if (points == null || points.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            TelemetryPoint p1 = points.get(i);
            TelemetryPoint p2 = points.get(i + 1);

            totalDistance += GeoDistance.haversineKm(p1.getLat(), p1.getLon(), p2.getLat(), p2.getLon());
        }
        return totalDistance;
    }
}
