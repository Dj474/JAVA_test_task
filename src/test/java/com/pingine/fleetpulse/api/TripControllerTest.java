package com.pingine.fleetpulse.api;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @Test
    void get404StatusWhenVehicleNotExists() throws Exception {
        String vehicleId = "unknown-id";

        when(tripService.getLastTrip(vehicleId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/trips/{vehicleId}/last-trip", vehicleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJSONWhenVehicleExists() throws Exception {
        String vehicleId = "v-123";
        VehicleResponse vehicleDto = VehicleResponse.builder()
                .id(vehicleId)
                .licensePlate("AB-123-CD")
                .model("Tesla Model 3")
                .vin("1234567890ABCDEFG")
                .driverName("Илон Маск")
                .build();

        // 2. Данные для Точек (минимум две)
        Instant start = Instant.parse("2026-05-01T10:00:00Z");
        Instant end = Instant.parse("2026-05-01T10:10:00Z");

        TripResponse.PointDto p1 = TripResponse.PointDto.builder()
                .ts(start).lat(55.7558).lon(37.6173).speedKph(0.0).build();
        TripResponse.PointDto p2 = TripResponse.PointDto.builder()
                .ts(end).lat(55.7590).lon(37.6250).speedKph(45.0).build();

        // 3. Собираем финальный TripResponse
        TripResponse response = TripResponse.builder()
                .vehicle(vehicleDto)
                .startedAt(start)
                .endedAt(end)
                .distanceKm(0.85)
                .avgSpeedKph(5.1)
                .pointCount(2)
                .points(List.of(p1, p2))
                .build();

        when(tripService.getLastTrip(vehicleId)).thenReturn(response);

        // 4. Проверка
        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", vehicleId))
                .andExpect(status().isOk())
                // Проверка вложенного объекта vehicle
                .andExpect(jsonPath("$.vehicle.id").value(vehicleId))
                .andExpect(jsonPath("$.vehicle.licensePlate").value("AB-123-CD"))
                .andExpect(jsonPath("$.vehicle.model").value("Tesla Model 3"))
                .andExpect(jsonPath("$.vehicle.vin").value("1234567890ABCDEFG"))
                .andExpect(jsonPath("$.vehicle.driverName").value("Илон Маск"))

                // Проверка метаданных поездки
                .andExpect(jsonPath("$.startedAt").value("2026-05-01T10:00:00Z"))
                .andExpect(jsonPath("$.endedAt").value("2026-05-01T10:10:00Z"))
                .andExpect(jsonPath("$.distanceKm").value(0.85))
                .andExpect(jsonPath("$.avgSpeedKph").value(5.1))
                .andExpect(jsonPath("$.pointCount").value(2))

                // Проверка массива точек
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points.length()").value(2))

                // Проверка первой точки (Старт)
                .andExpect(jsonPath("$.points[0].ts").value("2026-05-01T10:00:00Z"))
                .andExpect(jsonPath("$.points[0].lat").value(55.7558))
                .andExpect(jsonPath("$.points[0].lon").value(37.6173))
                .andExpect(jsonPath("$.points[0].speedKph").value(0.0))

                // Проверка второй точки (Финиш)
                .andExpect(jsonPath("$.points[1].ts").value("2026-05-01T10:10:00Z"))
                .andExpect(jsonPath("$.points[1].lat").value(55.7590))
                .andExpect(jsonPath("$.points[1].lon").value(37.6250))
                .andExpect(jsonPath("$.points[1].speedKph").value(45.0));
    }

}
