package com.tesis.nsdemo.impl;

import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelDemoPlannerTest {

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
        assertEquals("book-flight-madr-pari", result.actions().get(0).name());
        assertEquals("book-hotel", result.actions().get(1).name());
        assertEquals("visit-attraction", result.actions().get(2).name());
    }
}


