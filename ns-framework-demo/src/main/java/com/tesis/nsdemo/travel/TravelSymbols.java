package com.tesis.nsdemo.travel;

import java.text.Normalizer;
import java.util.Locale;

public final class TravelSymbols {
    private TravelSymbols() {
    }

    public static String travelerSymbol(String travelerId) {
        return sanitize("traveler_" + travelerId);
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "item";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            return "item";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized = "x_" + normalized;
        }
        return normalized;
    }
}

