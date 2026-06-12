package com.tesis.nsdemo.impl;

import com.tesis.nsframework.core.model.*;
import com.tesis.nsframework.core.port.Planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DemoPlanner implements Planner {
    @Override
    public PlanResult plan(String domainPddl, PlanningProblem problem, PlannerOptions options) {
        List<PlannedAction> actions = new ArrayList<>();
        String goal = problem.goalExpression();
        boolean registered = problem.initFacts().contains("(registered user1)");

        if (goal.contains("purchased")) {
            if (!registered) {
                actions.add(new PlannedAction("register-user", List.of("user1"), Map.of()));
            }
            actions.add(new PlannedAction("buy-product", List.of("user1", "product1"), Map.of()));
            return PlanResult.success(actions, "; demo plan for purchase");
        }

        if (goal.contains("appointment-booked")) {
            if (!registered) {
                actions.add(new PlannedAction("register-user", List.of("user1"), Map.of()));
            }
            actions.add(new PlannedAction("book-appointment", List.of("user1", "slot1"), Map.of()));
            return PlanResult.success(actions, "; demo plan for appointment");
        }

        if (goal.contains("registered")) {
            actions.add(new PlannedAction("register-user", List.of("user1"), Map.of()));
            return PlanResult.success(actions, "; demo plan for registration");
        }

        return PlanResult.failure("Unsupported goal in demo planner");
    }
}
