package com.tesis.nsframework.core.model;

import java.time.Duration;

public record PlannerOptions(Duration timeout, String strategy) {
    public static PlannerOptions defaults() {
        return new PlannerOptions(Duration.ofSeconds(30), "astar(lmcut())");
    }
}
