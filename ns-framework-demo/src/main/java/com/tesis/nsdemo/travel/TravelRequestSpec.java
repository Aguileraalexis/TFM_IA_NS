package com.tesis.nsdemo.travel;

import com.tesis.nsframework.core.model.GoalSpec;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record TravelRequestSpec(
        String travelerId,
        String travelerSymbol,
        String originCityId,
        List<String> targetCityIds,
        List<String> attractionIds,
        LocalDate travelDate
) {
    public static TravelRequestSpec fromGoalSpec(GoalSpec goalSpec) {
        Map<String, String> parameters = goalSpec.parameters();
        String travelerId = parameters.getOrDefault("travelerId", "1");
        return new TravelRequestSpec(
                travelerId,
                parameters.getOrDefault("travelerSymbol", TravelSymbols.travelerSymbol(travelerId)),
                parameters.get("originCityId"),
                split(parameters.get("targetCityIds")),
                split(parameters.get("requestedAttractionIds")),
                LocalDate.parse(parameters.get("travelDate"))
        );
    }

    private static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Stream.of(raw.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}

