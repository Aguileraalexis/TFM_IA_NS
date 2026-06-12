package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.config.TravelDemoProperties;
import com.tesis.nsdemo.travel.TravelCatalogService;
import com.tesis.nsdemo.travel.TravelCatalogSnapshot;
import com.tesis.nsdemo.travel.TravelSymbols;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.port.IntentInterpreter;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TravelIntentInterpreter implements IntentInterpreter {
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");

    private final TravelCatalogService travelCatalogService;
    private final TravelDemoProperties properties;

    public TravelIntentInterpreter(TravelCatalogService travelCatalogService, TravelDemoProperties properties) {
        this.travelCatalogService = travelCatalogService;
        this.properties = properties;
    }

    @Override
    public InterpretationResult interpret(String userInput, DomainMetadata domainMetadata) {
        if (userInput == null || userInput.isBlank()) {
            throw new FrameworkException("Travel prompt must not be blank");
        }

        TravelCatalogSnapshot snapshot = travelCatalogService.fetchSnapshot();
        String normalizedInput = normalize(userInput);
        List<Match> cityMatches = findCityMatches(normalizedInput, snapshot);
        List<Match> attractionMatches = findAttractionMatches(normalizedInput, snapshot);

        String originCityId = resolveOriginCityId(normalizedInput, cityMatches)
                .orElseGet(() -> cityMatches.isEmpty() ? null : cityMatches.getFirst().id());
        if (originCityId == null) {
            throw new FrameworkException("No se pudo inferir la ciudad de origen desde el prompt");
        }

        List<String> targetCityIds = new ArrayList<>();
        for (Match cityMatch : cityMatches) {
            if (!cityMatch.id().equals(originCityId) && !targetCityIds.contains(cityMatch.id())) {
                targetCityIds.add(cityMatch.id());
            }
        }

        List<String> attractionIds = new ArrayList<>();
        for (Match attractionMatch : attractionMatches) {
            attractionIds.add(attractionMatch.id());
            if (!attractionMatch.cityId().equals(originCityId) && !targetCityIds.contains(attractionMatch.cityId())) {
                targetCityIds.add(attractionMatch.cityId());
            }
        }

        if (targetCityIds.isEmpty() && attractionIds.isEmpty()) {
            throw new FrameworkException("No se pudieron inferir ciudades o atractivos destino desde el prompt");
        }

        LocalDate travelDate = extractTravelDate(normalizedInput)
                .orElse(LocalDate.now().plusDays(properties.getDefaultTravelDateOffsetDays()));
        String travelerId = properties.getDefaultTravelerId();

        Map<String, Object> entities = new LinkedHashMap<>();
        entities.put("travelerId", travelerId);
        entities.put("travelerSymbol", TravelSymbols.travelerSymbol(travelerId));
        entities.put("originCityId", originCityId);
        entities.put("targetCityIds", String.join("|", targetCityIds));
        entities.put("requestedAttractionIds", String.join("|", attractionIds));
        entities.put("travelDate", travelDate.toString());

        return new InterpretationResult("plan_trip", entities, Map.of(), 0.92, userInput);
    }

    private Optional<String> resolveOriginCityId(String normalizedInput, List<Match> cityMatches) {
        for (Match cityMatch : cityMatches) {
            if (normalizedInput.contains("desde " + cityMatch.normalizedName())
                    || normalizedInput.contains("from " + cityMatch.normalizedName())
                    || normalizedInput.contains("origen " + cityMatch.normalizedName())) {
                return Optional.of(cityMatch.id());
            }
        }
        return Optional.empty();
    }

    private List<Match> findCityMatches(String normalizedInput, TravelCatalogSnapshot snapshot) {
        List<Match> matches = new ArrayList<>();
        snapshot.cities().forEach(city -> {
            String normalizedName = normalize(city.nombre());
            int index = normalizedInput.indexOf(normalizedName);
            if (index >= 0) {
                matches.add(new Match(city.id(), null, normalizedName, index));
            }
        });
        matches.sort(java.util.Comparator.comparingInt(Match::position));
        return deduplicateById(matches);
    }

    private List<Match> findAttractionMatches(String normalizedInput, TravelCatalogSnapshot snapshot) {
        List<Match> matches = new ArrayList<>();
        snapshot.attractions().forEach(attraction -> {
            String normalizedName = normalize(attraction.nombre());
            int index = normalizedInput.indexOf(normalizedName);
            if (index >= 0) {
                matches.add(new Match(attraction.id(), attraction.ciudadId(), normalizedName, index));
            }
        });
        matches.sort(java.util.Comparator.comparingInt(Match::position));
        return deduplicateById(matches);
    }

    private List<Match> deduplicateById(List<Match> matches) {
        Map<String, Match> ordered = new LinkedHashMap<>();
        for (Match match : matches) {
            ordered.putIfAbsent(match.id(), match);
        }
        return List.copyOf(ordered.values());
    }

    private Optional<LocalDate> extractTravelDate(String normalizedInput) {
        Matcher matcher = DATE_PATTERN.matcher(normalizedInput);
        if (matcher.find()) {
            return Optional.of(LocalDate.parse(matcher.group(1)));
        }
        return Optional.empty();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record Match(String id, String cityId, String normalizedName, int position) {
    }
}

