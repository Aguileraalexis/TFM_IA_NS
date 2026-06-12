package com.tesis.nsframework.config.binding;

import java.util.Collections;
import java.util.Map;

public record ActionBinding(
        String action,
        String endpoint,
        String method,
        Map<String, String> headers,
        Map<String, Object> body,
        String successCondition,
        Map<String, String> effectsMapping
) {
    public ActionBinding {
        headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
        body = body == null ? Collections.emptyMap() : Collections.unmodifiableMap(body);
        effectsMapping = effectsMapping == null ? Collections.emptyMap() : Collections.unmodifiableMap(effectsMapping);
    }
}
