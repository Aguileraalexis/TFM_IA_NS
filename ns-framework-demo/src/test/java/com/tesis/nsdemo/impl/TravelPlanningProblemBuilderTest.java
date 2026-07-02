package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import com.tesis.nsdemo.travel.TravelCatalogService;
import com.tesis.nsdemo.travel.TravelCatalogSnapshot;
import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.model.SymbolicState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelPlanningProblemBuilderTest {

    @Test
    void shouldBuildProblemUsingLiveLikeCatalogData() {
        TravelCatalogService catalogService = new TravelCatalogService(null, null, null) {
            @Override
            public TravelCatalogSnapshot fetchSnapshot(java.time.LocalDate travelDate) {
                return new TravelCatalogSnapshot(
                        List.of(new CityDto("MADR", "Madrid", "ESPA"), new CityDto("PARI", "Paris", "FRAN")),
                        List.of(new AttractionDto("AT017", "PARI", "Torre Eiffel", "Monumento")),
                        List.of(new FlightDto("V001", "MADR", "PARI", "AL01")),
                        List.of(new HotelDto("HT016", "Hilton Paris Opera", "PARI")),
                        Map.of("PARI", List.of(new HotelDto("HT016", "Hilton Paris Opera", "PARI"))),
                        Map.of("PARI", List.of(new AttractionDto("AT017", "PARI", "Torre Eiffel", "Monumento")))
                );
            }
        };

        TravelPlanningProblemBuilder builder = new TravelPlanningProblemBuilder(catalogService);
        GoalSpec goal = new GoalSpec(
                "travel_goal",
                Map.of(
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1",
                        "originCityId", "MADR",
                        "targetCityIds", "PARI",
                        "requestedAttractionIds", "AT017",
                        "travelDate", "2026-07-10"
                ),
                "travel-dynamic",
                "trip"
        );

        PlanningProblem problem = builder.build(new SymbolicState(), goal);

        assertTrue(problem.initFacts().contains("(flight-available MADR PARI)"));
        assertTrue(problem.initFacts().contains("(hotel-in-city HT016 PARI)"));
        assertTrue(problem.goalExpression().contains("(visited-city PARI)"));
        assertTrue(problem.goalExpression().contains("(hotel-booked traveler_1 HT016 PARI)"));
        assertTrue(problem.goalExpression().contains("(visited-attraction AT017)"));
        // Verifica que el objeto del viajero se declara con su tipo PDDL
        assertTrue(problem.objects().contains("traveler_1 - traveler"),
                "Los objetos del problema deben usar sintaxis PDDL tipada para compatibilidad con :typing");
    }

    @Test
    void shouldExcludeBlacklistedFlightRoutesAndHotelsFromPlanningProblem() {
        TravelCatalogService catalogService = new TravelCatalogService(null, null, null) {
            @Override
            public TravelCatalogSnapshot fetchSnapshot(java.time.LocalDate travelDate) {
                HotelDto blockedHotel = new HotelDto("HT020", "Hotel vetado", "LOGR");
                HotelDto alternativeHotel = new HotelDto("HT021", "Hotel alternativo", "LOGR");
                return new TravelCatalogSnapshot(
                        List.of(new CityDto("MADR", "Madrid", "ESPA"), new CityDto("PARI", "Paris", "FRAN"), new CityDto("LOGR", "Logrono", "ESPA")),
                        List.of(new AttractionDto("AT021", "LOGR", "Calle Laurel", "Gastronomia")),
                        List.of(
                                new FlightDto("V001", "MADR", "PARI", "AL01"),
                                new FlightDto("V002", "PARI", "LOGR", "AL02"),
                                new FlightDto("V003", "MADR", "LOGR", "AL03")
                        ),
                        List.of(blockedHotel, alternativeHotel),
                        Map.of("LOGR", List.of(blockedHotel, alternativeHotel)),
                        Map.of("LOGR", List.of(new AttractionDto("AT021", "LOGR", "Calle Laurel", "Gastronomia")))
                );
            }
        };

        TravelPlanningProblemBuilder builder = new TravelPlanningProblemBuilder(catalogService);
        GoalSpec goal = new GoalSpec(
                "travel_goal",
                Map.of(
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1",
                        "originCityId", "MADR",
                        "targetCityIds", "LOGR",
                        "requestedAttractionIds", "AT021",
                        "travelDate", "2026-07-10"
                ),
                "travel-dynamic",
                "trip"
        );
        TravelPlanningBlacklist blacklist = new TravelPlanningBlacklist();
        blacklist.blacklistFlightRoute("MADR", "PARI");
        blacklist.blacklistHotel("HT020");

        PlanningProblem problem = builder.build(new SymbolicState(), goal, blacklist);

        assertTrue(problem.initFacts().contains("(flight-available PARI LOGR)"));
        assertTrue(problem.initFacts().contains("(flight-available MADR LOGR)"));
        assertFalse(problem.initFacts().contains("(flight-available MADR PARI)"));
        assertTrue(problem.initFacts().contains("(hotel-in-city HT021 LOGR)"));
        assertFalse(problem.initFacts().contains("(hotel-in-city HT020 LOGR)"));
        assertTrue(problem.goalExpression().contains("(hotel-booked traveler_1 HT021 LOGR)"));
        assertEquals(Set.of("MADR->PARI"), problem.metadata().get("blacklistedFlightRoutes"));
        assertEquals(Set.of("HT020"), problem.metadata().get("blacklistedHotelIds"));
    }
}
