package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.travel.TravelSymbols;
import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.port.Planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback Java-based travel planner used in tests and local development
 * when no Docker environment is available.
 * Not registered as a Spring bean; the real bean is {@code DockerPlanner}
 * configured in {@code DemoBeansConfig}.
 */
public class TravelDemoPlanner implements Planner {
    @Override
    @SuppressWarnings("unchecked")
    public PlanResult plan(String domainPddl, PlanningProblem problem, PlannerOptions options) {
        Map<String, Object> metadata = problem.metadata();
        String travelerId = String.valueOf(metadata.get("travelerId"));
        String travelerSymbol = String.valueOf(metadata.get("travelerSymbol"));
        String currentCity = String.valueOf(metadata.get("originCityId"));
        String travelDate = String.valueOf(metadata.get("travelDate"));
        List<String> targetCityIds = (List<String>) metadata.getOrDefault("targetCityIds", List.of());
        Map<String, String> selectedHotelByCity = (Map<String, String>) metadata.getOrDefault("selectedHotelByCity", Map.of());
        Map<String, List<String>> attractionsByCity = (Map<String, List<String>>) metadata.getOrDefault("attractionsByCity", Map.of());
        Map<String, String> preferredAirlineByRoute = (Map<String, String>) metadata.getOrDefault("preferredAirlineByRoute", Map.of());

        List<PlannedAction> actions = new ArrayList<>();
        for (String targetCityId : targetCityIds) {
            if (!currentCity.equals(targetCityId)) {
                String routeKey = currentCity + "->" + targetCityId;
                String airlineId = preferredAirlineByRoute.get(routeKey);
                if (airlineId == null) {
                    return PlanResult.failure("No hay vuelo directo disponible entre " + currentCity + " y " + targetCityId);
                }
                Map<String, Object> metadataMap = new LinkedHashMap<>();
                metadataMap.put("usuarioId", travelerId);
                metadataMap.put("travelerSymbol", travelerSymbol);
                metadataMap.put("aerolineaId", airlineId);
                metadataMap.put("ciudadOrigenId", currentCity);
                metadataMap.put("ciudadDestinoId", targetCityId);
                metadataMap.put("fecha", travelDate);
                String actionName = "book-flight-" + TravelSymbols.sanitize(currentCity) + "-" + TravelSymbols.sanitize(targetCityId);
                actions.add(new PlannedAction(actionName, List.of(travelerSymbol, currentCity, targetCityId), metadataMap));
                currentCity = targetCityId;
            }

            String hotelId = selectedHotelByCity.get(targetCityId);
            if (hotelId != null) {
                Map<String, Object> metadataMap = new LinkedHashMap<>();
                metadataMap.put("usuarioId", travelerId);
                metadataMap.put("travelerSymbol", travelerSymbol);
                metadataMap.put("hotelId", hotelId);
                metadataMap.put("ciudadId", targetCityId);
                metadataMap.put("fecha", travelDate);
                actions.add(new PlannedAction("book-hotel", List.of(travelerSymbol, hotelId, targetCityId), metadataMap));
            }

            for (String attractionId : attractionsByCity.getOrDefault(targetCityId, List.of())) {
                Map<String, Object> metadataMap = new LinkedHashMap<>();
                metadataMap.put("travelerSymbol", travelerSymbol);
                metadataMap.put("attractionId", attractionId);
                metadataMap.put("ciudadId", targetCityId);
                actions.add(new PlannedAction("visit-attraction", List.of(travelerSymbol, attractionId, targetCityId), metadataMap));
            }
        }

        return actions.isEmpty()
                ? PlanResult.failure("No se pudieron construir acciones para el viaje solicitado")
                : PlanResult.success(actions, "; dynamically generated travel plan");
    }
}
