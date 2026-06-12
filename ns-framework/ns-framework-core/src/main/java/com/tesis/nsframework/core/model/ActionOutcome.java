package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.Map;

public record ActionOutcome(
        boolean success,
        int statusCode,
        String message,
        Map<String, Object> payload,
        Map<String, String> observedEffects
) {
    public ActionOutcome {
        payload = payload == null ? Collections.emptyMap() : Collections.unmodifiableMap(payload);
        observedEffects = observedEffects == null ? Collections.emptyMap() : Collections.unmodifiableMap(observedEffects);
    }

    public static ActionOutcome success(int statusCode, String message, Map<String, Object> payload, Map<String, String> observedEffects) {
        return new ActionOutcome(true, statusCode, message, payload, observedEffects);
    }

    public static ActionOutcome failure(int statusCode, String message, Map<String, Object> payload) {
        return new ActionOutcome(false, statusCode, message, payload, Map.of());
    }
}
