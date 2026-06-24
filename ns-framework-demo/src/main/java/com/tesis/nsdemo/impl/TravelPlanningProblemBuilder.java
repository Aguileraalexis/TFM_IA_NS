package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import com.tesis.nsdemo.travel.TravelCatalogService;
import com.tesis.nsdemo.travel.TravelCatalogSnapshot;
import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsdemo.travel.TravelRequestSpec;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.PlanningProblemBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TravelPlanningProblemBuilder implements PlanningProblemBuilder {
    private final TravelCatalogService travelCatalogService;

    public TravelPlanningProblemBuilder(TravelCatalogService travelCatalogService) {
        this.travelCatalogService = travelCatalogService;
    }

    @Override
    public PlanningProblem build(SymbolicState state, GoalSpec goalSpec) {
        TravelRequestSpec request = TravelRequestSpec.fromGoalSpec(goalSpec);
        TravelCatalogSnapshot snapshot = travelCatalogService.fetchSnapshot(request.travelDate());

        if (request.originCityId() == null || request.targetCityIds().isEmpty()) {
            throw new FrameworkException("Travel planning requires originCityId and at least one target city");
        }

        Map<String, String> selectedHotelByCity = new LinkedHashMap<>();
        for (String cityId : request.targetCityIds()) {
            HotelDto hotel = snapshot.hotelsByCity().getOrDefault(cityId, List.of()).stream()
                    .sorted(java.util.Comparator.comparing(HotelDto::id))
                    .findFirst()
                    .orElseThrow(() -> new FrameworkException("No hay hoteles configurados para la ciudad " + cityId));
            selectedHotelByCity.put(cityId, hotel.id());
        }

        Map<String, List<String>> attractionsByCity = new LinkedHashMap<>();
        Set<String> requestedAttractions = Set.copyOf(request.attractionIds());
        for (String cityId : request.targetCityIds()) {
            List<String> attractionIds = snapshot.attractionsByCity().getOrDefault(cityId, List.of()).stream()
                    .map(AttractionDto::id)
                    .filter(requestedAttractions::contains)
                    .toList();
            attractionsByCity.put(cityId, attractionIds);
        }

        Map<String, String> preferredAirlineByRoute = new LinkedHashMap<>();
        Set<String> initFacts = new LinkedHashSet<>(state.facts());
        initFacts.add("(at " + request.travelerSymbol() + " " + request.originCityId() + ")");
        initFacts.add("(visited-city " + request.originCityId() + ")");
        for (FlightDto flight : snapshot.flights()) {
            initFacts.add("(flight-available " + flight.ciudadOrigenId() + " " + flight.ciudadDestinoId() + ")");
            preferredAirlineByRoute.putIfAbsent(routeKey(flight.ciudadOrigenId(), flight.ciudadDestinoId()), flight.aerolineaId());
        }
        for (HotelDto hotel : snapshot.hotels()) {
            initFacts.add("(hotel-in-city " + hotel.id() + " " + hotel.ciudadId() + ")");
        }
        for (AttractionDto attraction : snapshot.attractions()) {
            initFacts.add("(attraction-in-city " + attraction.id() + " " + attraction.ciudadId() + ")");
        }

        List<String> goalParts = new ArrayList<>();
        for (String cityId : request.targetCityIds()) {
            goalParts.add("(visited-city " + cityId + ")");
            goalParts.add("(hotel-booked " + request.travelerSymbol() + " " + selectedHotelByCity.get(cityId) + " " + cityId + ")");
            attractionsByCity.getOrDefault(cityId, List.of())
                    .forEach(attractionId -> goalParts.add("(visited-attraction " + attractionId + ")"));
        }
        String goalExpression = goalParts.size() == 1 ? goalParts.getFirst() : "(and " + String.join(" ", goalParts) + ")";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("travelerId", request.travelerId());
        metadata.put("travelerSymbol", request.travelerSymbol());
        metadata.put("originCityId", request.originCityId());
        metadata.put("targetCityIds", request.targetCityIds());
        metadata.put("travelDate", request.travelDate().toString());
        metadata.put("selectedHotelByCity", selectedHotelByCity);
        metadata.put("attractionsByCity", attractionsByCity);
        metadata.put("preferredAirlineByRoute", preferredAirlineByRoute);

        return new PlanningProblem(
                "travel-problem-" + System.currentTimeMillis(),
                TravelDomainPddlGenerator.DOMAIN_NAME,
                // El viajero debe declararse con su tipo para que el PDDL sea valido con :typing
                Set.of(request.travelerSymbol() + " - traveler"),
                initFacts,
                goalExpression,
                metadata
        );
    }

    private String routeKey(String origin, String destination) {
        return origin + "->" + destination;
    }
}
