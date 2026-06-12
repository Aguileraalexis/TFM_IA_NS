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

import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelPlanningProblemBuilderTest {

    @Test
    void shouldBuildProblemUsingLiveLikeCatalogData() {
        TravelCatalogService catalogService = new TravelCatalogService(null, null, null) {
            @Override
            public TravelCatalogSnapshot fetchSnapshot() {
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
        // Verify the traveler object is declared with its PDDL type
        assertTrue(problem.objects().contains("traveler_1 - traveler"),
                "Problem objects must use typed PDDL syntax for :typing compatibility");
    }
}

