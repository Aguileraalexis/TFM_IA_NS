package com.tesis.nsdemo.impl;

import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.ExecutionContext;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.port.ActionExecutor;

import java.util.HashMap;
import java.util.Map;

public class DemoActionExecutor implements ActionExecutor {
    @Override
    public ActionOutcome execute(PlannedAction action, ExecutionContext context) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> effects = new HashMap<>();

        switch (action.name()) {
            case "register-user" -> {
                payload.put("registered", true);
                effects.put("(registered user1)", "true");
                return ActionOutcome.success(201, "User registered", payload, effects);
            }
            case "buy-product" -> {
                payload.put("purchaseId", "purchase-001");
                effects.put("(purchased user1 product1)", "true");
                return ActionOutcome.success(201, "Product purchased", payload, effects);
            }
            case "book-appointment" -> {
                payload.put("appointmentId", "appt-001");
                effects.put("(appointment-booked user1 slot1)", "true");
                return ActionOutcome.success(201, "Appointment booked", payload, effects);
            }
            default -> {
                return ActionOutcome.failure(400, "Unsupported action in demo executor", Map.of("action", action.name()));
            }
        }
    }
}
