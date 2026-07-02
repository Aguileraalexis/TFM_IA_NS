package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.client.FlightServiceClient;
import com.tesis.nsdemo.client.HotelServiceClient;
import com.tesis.nsdemo.client.dto.FlightReservationDto;
import com.tesis.nsdemo.client.dto.HotelReservationDto;
import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.ExecutionContext;
import com.tesis.nsframework.core.model.PlannedAction;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelFeignActionExecutorTest {

    private final FlightServiceClient flightClient = new FlightServiceClient() {
        @Override
        public List<com.tesis.nsdemo.client.dto.CityDto> getCities() { return List.of(); }
        @Override
        public List<com.tesis.nsdemo.client.dto.FlightDto> getFlights(LocalDate fecha) { return List.of(); }
        @Override
        public FlightReservationDto createReservation(com.tesis.nsdemo.client.dto.FlightReservationRequest request) {
            return new FlightReservationDto("RV-AABB", request.usuarioId(), "V001",
                    request.aerolineaId(), request.ciudadOrigenId(), request.ciudadDestinoId(),
                    request.fecha(), "BOOKED");
        }
    };

    private final HotelServiceClient hotelClient = new HotelServiceClient() {
        @Override
        public List<com.tesis.nsdemo.client.dto.CityDto> getCities() { return List.of(); }
        @Override
        public List<com.tesis.nsdemo.client.dto.HotelDto> getHotels(String ciudadId, LocalDate fecha) { return List.of(); }
        @Override
        public HotelReservationDto createReservation(com.tesis.nsdemo.client.dto.HotelReservationRequest req) {
            return new HotelReservationDto("RS-1234", req.usuarioId(), "RM0052", req.hotelId(), "BOOKED", req.fecha());
        }
    };

    private final TravelFeignActionExecutor executor = new TravelFeignActionExecutor(flightClient, hotelClient);

    private ExecutionContext baseContext() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("travelerId", "1");
        ctx.put("travelerSymbol", "traveler_1");
        ctx.put("travelDate", "2026-07-10");
        ctx.put("preferredAirlineByRoute", Map.of("MADR->PARI", "AL01"));
        return ctx;
    }

    @Test
    void shouldExecuteFlightActionFromArguments() {
        PlannedAction action = new PlannedAction("book-flight", List.of("traveler_1", "madr", "pari"), Map.of());
        ActionOutcome outcome = executor.execute(action, baseContext());
        assertTrue(outcome.success());
        assertEquals("RV-AABB", outcome.payload().get("reservationId"));
        assertTrue(outcome.observedEffects().containsKey("(flight-booked traveler_1 MADR PARI)"));
    }

    @Test
    void shouldExecuteHotelActionFromArguments() {
        // Los argumentos llegan en minusculas, tal como los entrega PlanTextParser
        PlannedAction action = new PlannedAction("book-hotel", List.of("traveler_1", "ht016", "pari"), Map.of());
        ActionOutcome outcome = executor.execute(action, baseContext());
        assertTrue(outcome.success());
        assertEquals("RS-1234", outcome.payload().get("reservationId"));
        assertEquals("HT016", outcome.payload().get("hotelId"));
        assertTrue(outcome.observedEffects().containsKey("(hotel-booked traveler_1 HT016 PARI)"));
    }

    @Test
    void shouldExecuteAttractionActionFromArguments() {
        PlannedAction action = new PlannedAction("visit-attraction", List.of("traveler_1", "at017", "pari"), Map.of());
        ActionOutcome outcome = executor.execute(action, baseContext());
        assertTrue(outcome.success());
        assertEquals("AT017", outcome.payload().get("attractionId"));
        assertTrue(outcome.observedEffects().containsKey("(visited-attraction AT017)"));
    }

    @Test
    void shouldReturnFailureOutcomeWhenFlightReservationConflicts() {
        FlightServiceClient conflictFlightClient = new FlightServiceClient() {
            @Override
            public List<com.tesis.nsdemo.client.dto.CityDto> getCities() { return List.of(); }

            @Override
            public List<com.tesis.nsdemo.client.dto.FlightDto> getFlights(LocalDate fecha) { return List.of(); }

            @Override
            public FlightReservationDto createReservation(com.tesis.nsdemo.client.dto.FlightReservationRequest request) {
                throw conflictFeign(409, "Vuelo sin disponibilidad");
            }
        };

        TravelFeignActionExecutor conflictExecutor = new TravelFeignActionExecutor(conflictFlightClient, hotelClient);
        PlannedAction action = new PlannedAction("book-flight", List.of("traveler_1", "madr", "pari"), Map.of());

        ActionOutcome outcome = conflictExecutor.execute(action, baseContext());

        assertFalse(outcome.success());
        assertEquals(409, outcome.statusCode());
        assertTrue(outcome.message().contains("Vuelo sin disponibilidad"));
    }

    @Test
    void shouldReturnFailureOutcomeWhenHotelReservationConflicts() {
        HotelServiceClient conflictHotelClient = new HotelServiceClient() {
            @Override
            public List<com.tesis.nsdemo.client.dto.CityDto> getCities() { return List.of(); }

            @Override
            public List<com.tesis.nsdemo.client.dto.HotelDto> getHotels(String ciudadId, LocalDate fecha) { return List.of(); }

            @Override
            public HotelReservationDto createReservation(com.tesis.nsdemo.client.dto.HotelReservationRequest req) {
                throw conflictFeign(409, "Hotel sin disponibilidad");
            }
        };

        TravelFeignActionExecutor conflictExecutor = new TravelFeignActionExecutor(flightClient, conflictHotelClient);
        PlannedAction action = new PlannedAction("book-hotel", List.of("traveler_1", "ht016", "pari"), Map.of());

        ActionOutcome outcome = conflictExecutor.execute(action, baseContext());

        assertFalse(outcome.success());
        assertEquals(409, outcome.statusCode());
        assertTrue(outcome.message().contains("Hotel sin disponibilidad"));
    }

    @Test
    void shouldReturnFailureOutcomeWhenFlightReservationServiceIsUnavailable() {
        FlightServiceClient unavailableFlightClient = new FlightServiceClient() {
            @Override
            public List<com.tesis.nsdemo.client.dto.CityDto> getCities() { return List.of(); }

            @Override
            public List<com.tesis.nsdemo.client.dto.FlightDto> getFlights(LocalDate fecha) { return List.of(); }

            @Override
            public FlightReservationDto createReservation(com.tesis.nsdemo.client.dto.FlightReservationRequest request) {
                throw new IllegalStateException("Connection refused");
            }
        };

        TravelFeignActionExecutor unavailableExecutor = new TravelFeignActionExecutor(unavailableFlightClient, hotelClient);
        PlannedAction action = new PlannedAction("book-flight", List.of("traveler_1", "madr", "pari"), Map.of());

        ActionOutcome outcome = unavailableExecutor.execute(action, baseContext());

        assertFalse(outcome.success());
        assertEquals(503, outcome.statusCode());
        assertTrue(outcome.message().contains("servicio externo no esta disponible"));
    }

    private FeignException conflictFeign(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://mock-service/conflict",
                Map.of(),
                null,
                null,
                null
        );
        Response response = Response.builder()
                .status(status)
                .reason("Conflict")
                .request(request)
                .headers(Map.<String, Collection<String>>of())
                .body(body, java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("POST http://mock-service/conflict", response);
    }
}


