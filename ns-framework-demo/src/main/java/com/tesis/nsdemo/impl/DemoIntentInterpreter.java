package com.tesis.nsdemo.impl;

import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.port.IntentInterpreter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DemoIntentInterpreter implements IntentInterpreter {
    @Override
    public InterpretationResult interpret(String userInput, DomainMetadata domainMetadata) {
        String normalized = userInput.toLowerCase(Locale.ROOT);
        Map<String, Object> entities = new HashMap<>();
        entities.put("user", normalized.contains("user1") ? "user1" : "user1");

        if (normalized.contains("buy") || normalized.contains("compr")) {
            entities.put("product", "product1");
            return new InterpretationResult("buy_product", entities, Map.of(), 0.95, userInput);
        }
        if (normalized.contains("appoint") || normalized.contains("cita") || normalized.contains("reserv")) {
            entities.put("slot", "slot1");
            return new InterpretationResult("book_appointment", entities, Map.of(), 0.95, userInput);
        }
        return new InterpretationResult("register_user", entities, Map.of(), 0.80, userInput);
    }
}
