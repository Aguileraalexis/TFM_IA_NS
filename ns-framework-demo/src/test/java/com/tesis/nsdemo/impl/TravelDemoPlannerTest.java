package com.tesis.nsdemo.impl;

import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelDemoPlannerTest {

    // Full route graph from the flight mock service (subset relevant to tests)
    private static final Map<String, String> ROUTES = Map.ofEntries(
            Map.entry("TORO->PARI", "AL03"),
            Map.entry("PARI->TORO", "AL03"),
            Map.entry("PARI->FRAK", "AL03"),
            Map.entry("FRAK->PARI", "AL03"),
            Map.entry("FRAK->MADR", "AL01"),
            Map.entry("MADR->FRAK", "AL01"),
            Map.entry("MADR->LOGR", "AL01"),
            Map.entry("LOGR->MADR", "AL01"),
            Map.entry("MADR->PARI", "AL01"),
            Map.entry("PARI->MADR", "AL01")
    );

    @Test
    void shouldCreateFlightHotelAndAttractionActions() {
        PlanningProblem problem = new PlanningProblem(
                "travel-problem-test",
                "travel-dynamic",
                Set.of("traveler_1"),
                Set.of("(at traveler_1 MADR)"),
                "(and (visited-city PARI) (hotel-booked traveler_1 HT016 PARI) (visited-attraction AT017))",
                Map.of(
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1",
                        "originCityId", "MADR",
                        "targetCityIds", List.of("PARI"),
                        "travelDate", "2026-07-10",
                        "selectedHotelByCity", Map.of("PARI", "HT016"),
                        "attractionsByCity", Map.of("PARI", List.of("AT017")),
                        "preferredAirlineByRoute", Map.of("MADR->PARI", "AL01")
                )
        );

        PlanResult result = new TravelDemoPlanner().plan("domain", problem, PlannerOptions.defaults());

        assertTrue(result.success());
        assertEquals(3, result.actions().size());
        assertEquals("book-flight", result.actions().get(0).name());
        assertEquals(List.of("traveler_1", "MADR", "PARI"), result.actions().get(0).arguments());
        assertEquals("book-hotel", result.actions().get(1).name());
        assertEquals("visit-attraction", result.actions().get(2).name());
    }

    @Test
    void shouldFindMultiHopRouteFromTorontoToLogro() {
        // TORO -> PARI -> FRAK -> MADR -> LOGR  (4 hops, no direct flight)
        PlanningProblem problem = new PlanningProblem(
                "travel-problem-test-multihop",
                "travel-dynamic",
                Set.of("traveler_1"),
                Set.of("(at traveler_1 TORO)"),
                "(and (visited-city LOGR) (hotel-booked traveler_1 HT001 LOGR) (visited-attraction AT021))",
                Map.of(
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1",
                        "originCityId", "TORO",
                        "targetCityIds", List.of("LOGR"),
                        "travelDate", "2026-07-10",
                        "selectedHotelByCity", Map.of("LOGR", "HT001"),
                        "attractionsByCity", Map.of("LOGR", List.of("AT021")),
                        "preferredAirlineByRoute", ROUTES
                )
        );

        PlanResult result = new TravelDemoPlanner().plan("domain", problem, PlannerOptions.defaults());

        assertTrue(result.success(), "Expected successful plan but got: " + (result.success() ? "" : result.message()));

        // Expect 3 flight hops (BFS shortest path): TORO->PARI->MADR->LOGR
        // (PARI->MADR is direct so BFS skips FRAK)
        // + 1 hotel + 1 attraction = 5 actions total
        long flightCount = result.actions().stream().filter(a -> a.name().equals("book-flight")).count();
        assertEquals(3, flightCount, "Expected 3 flight hops: TORO->PARI->MADR->LOGR");
        assertEquals("book-flight", result.actions().get(0).name());
        assertEquals(List.of("traveler_1", "TORO", "PARI"), result.actions().get(0).arguments());
        assertEquals("book-flight", result.actions().get(1).name());
        assertEquals(List.of("traveler_1", "PARI", "MADR"), result.actions().get(1).arguments());
        assertEquals("book-flight", result.actions().get(2).name());
        assertEquals(List.of("traveler_1", "MADR", "LOGR"), result.actions().get(2).arguments());
        assertEquals("book-hotel",       result.actions().get(3).name());
        assertEquals("visit-attraction", result.actions().get(4).name());

        // Hotel and attraction only at destination, not at intermediate stops
        result.actions().stream()
                .filter(a -> a.name().equals("book-hotel"))
                .forEach(a -> assertEquals("LOGR", a.arguments().get(2)));
    }

    @Test
    void shouldReturnFailureWhenNoRouteExists() {
        PlanningProblem problem = new PlanningProblem(
                "travel-problem-test-nroute",
                "travel-dynamic",
                Set.of("traveler_1"),
                Set.of("(at traveler_1 MADR)"),
                "(visited-city LOGR)",
                Map.of(
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1",
                        "originCityId", "MADR",
                        "targetCityIds", List.of("LOGR"),
                        "travelDate", "2026-07-10",
                        "selectedHotelByCity", Map.of("LOGR", "HT001"),
                        "attractionsByCity", Map.of(),
                        "preferredAirlineByRoute", Map.of()  // no routes at all
                )
        );

        PlanResult result = new TravelDemoPlanner().plan("domain", problem, PlannerOptions.defaults());

        assertFalse(result.success());
        assertTrue(result.message().contains("No hay ruta de vuelo disponible entre MADR y LOGR"));
    }
}


