package com.tesis.nsframework.core.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class SymbolicState {
    private final Set<String> facts = new LinkedHashSet<>();

    public SymbolicState() {
    }

    public SymbolicState(Set<String> initialFacts) {
        if (initialFacts != null) {
            facts.addAll(initialFacts.stream().filter(Objects::nonNull).toList());
        }
    }

    public Set<String> facts() {
        return Set.copyOf(facts);
    }

    public void addFact(String fact) {
        if (fact != null && !fact.isBlank()) {
            facts.add(fact);
        }
    }

    public void removeFact(String fact) {
        facts.remove(fact);
    }

    public boolean contains(String fact) {
        return facts.contains(fact);
    }

    public SymbolicState copy() {
        return new SymbolicState(facts);
    }
}
