package com.tesis.nsframework.planner.docker;

import com.tesis.nsframework.core.model.PlannedAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlanTextParser {
    public List<PlannedAction> parse(String rawPlan) {
        List<PlannedAction> actions = new ArrayList<>();
        if (rawPlan == null || rawPlan.isBlank()) {
            return actions;
        }

        for (String line : rawPlan.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith(";")) {
                continue;
            }
            int openIdx = trimmed.indexOf('(');
            int closeIdx = trimmed.indexOf(')');
            if (openIdx >= 0 && closeIdx > openIdx) {
                trimmed = trimmed.substring(openIdx + 1, closeIdx);
            }
            String[] parts = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
            if (parts.length > 0) {
                String name = parts[0];
                List<String> args = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    args.add(parts[i]);
                }
                actions.add(new PlannedAction(name, args, null));
            }
        }
        return actions;
    }
}
