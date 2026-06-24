package com.tesis.nsdemo.impl;

import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.port.Planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Planificador de viajes alternativo basado en Java, usado en pruebas y en
 * desarrollo local cuando no hay un entorno Docker disponible.
 * Encuentra rutas con escalas mediante BFS cuando no existe un vuelo directo.
 * No se registra como bean de Spring; el bean real es {@code DockerPlanner}
 * configurado en {@code DemoBeansConfig}.
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
                List<String[]> hops = findPath(currentCity, targetCityId, preferredAirlineByRoute);
                if (hops == null) {
                    return PlanResult.failure(
                            "No hay ruta de vuelo disponible entre " + currentCity + " y " + targetCityId);
                }
                for (String[] hop : hops) {
                    String from = hop[0];
                    String to = hop[1];
                    String airlineId = hop[2];
                    Map<String, Object> metadataMap = new LinkedHashMap<>();
                    metadataMap.put("usuarioId", travelerId);
                    metadataMap.put("travelerSymbol", travelerSymbol);
                    metadataMap.put("aerolineaId", airlineId);
                    metadataMap.put("ciudadOrigenId", from);
                    metadataMap.put("ciudadDestinoId", to);
                    metadataMap.put("fecha", travelDate);
                    actions.add(new PlannedAction("book-flight", List.of(travelerSymbol, from, to), metadataMap));
                }
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
                : PlanResult.success(actions, "; plan de viaje generado dinamicamente");
    }

    /**
     * Ejecuta BFS sobre el grafo de rutas de vuelo.
     * Devuelve una lista ordenada de tramos [{from, to, airlineId}] o null si no hay camino.
     */
    private List<String[]> findPath(String origin, String destination, Map<String, String> routeMap) {
        if (origin.equals(destination)) {
            return List.of();
        }
        // Construye la lista de adyacencia a partir de claves de ruta "FROM->TO"
        Map<String, List<String[]>> adj = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : routeMap.entrySet()) {
            String[] parts = entry.getKey().split("->", 2);
            if (parts.length == 2) {
                adj.computeIfAbsent(parts[0], k -> new ArrayList<>())
                        .add(new String[]{parts[1], entry.getValue()}); // [ciudadDestino, airlineId]
            }
        }
        // BFS
        Map<String, String> parentCity = new LinkedHashMap<>();  // ciudad -> ciudad previa
        Map<String, String> edgeAirline = new LinkedHashMap<>(); // "from->to" -> airlineId
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(origin);
        visited.add(origin);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(destination)) {
                return reconstructPath(origin, destination, parentCity, edgeAirline);
            }
            for (String[] next : adj.getOrDefault(current, List.of())) {
                String nextCity = next[0];
                String airline = next[1];
                if (!visited.contains(nextCity)) {
                    visited.add(nextCity);
                    parentCity.put(nextCity, current);
                    edgeAirline.put(current + "->" + nextCity, airline);
                    queue.add(nextCity);
                }
            }
        }
        return null; // no se encontro ruta
    }

    private List<String[]> reconstructPath(String origin, String destination,
                                           Map<String, String> parentCity,
                                           Map<String, String> edgeAirline) {
        List<String[]> path = new ArrayList<>();
        String city = destination;
        while (!city.equals(origin)) {
            String from = parentCity.get(city);
            String airline = edgeAirline.get(from + "->" + city);
            path.add(0, new String[]{from, city, airline});
            city = from;
        }
        return path;
    }
}
