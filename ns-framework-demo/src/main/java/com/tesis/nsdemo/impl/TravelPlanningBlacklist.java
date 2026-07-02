package com.tesis.nsdemo.impl;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class TravelPlanningBlacklist {
    private final Set<String> blacklistedFlightRoutes = new LinkedHashSet<>();
    private final Set<String> blacklistedHotelIds = new LinkedHashSet<>();

    public void blacklistFlightRoute(String originCityId, String destinationCityId) {
        blacklistedFlightRoutes.add(routeKey(originCityId, destinationCityId));
    }

    public void blacklistHotel(String hotelId) {
        if (hotelId != null && !hotelId.isBlank()) {
            blacklistedHotelIds.add(hotelId.trim().toUpperCase(Locale.ROOT));
        }
    }

    public boolean isFlightRouteBlacklisted(String originCityId, String destinationCityId) {
        return blacklistedFlightRoutes.contains(routeKey(originCityId, destinationCityId));
    }

    public boolean isHotelBlacklisted(String hotelId) {
        return hotelId != null && blacklistedHotelIds.contains(hotelId.trim().toUpperCase(Locale.ROOT));
    }

    public Set<String> blacklistedFlightRoutes() {
        return Set.copyOf(blacklistedFlightRoutes);
    }

    public Set<String> blacklistedHotelIds() {
        return Set.copyOf(blacklistedHotelIds);
    }

    private String routeKey(String originCityId, String destinationCityId) {
        return normalize(originCityId) + "->" + normalize(destinationCityId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
