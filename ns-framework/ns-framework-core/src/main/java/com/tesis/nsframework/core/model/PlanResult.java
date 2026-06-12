package com.tesis.nsframework.core.model;

import java.util.List;

public record PlanResult(boolean success, List<PlannedAction> actions, String rawPlan, String message) {
    public PlanResult {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static PlanResult success(List<PlannedAction> actions, String rawPlan) {
        return new PlanResult(true, actions, rawPlan, "OK");
    }

    public static PlanResult failure(String message) {
        return new PlanResult(false, List.of(), "", message);
    }
}
