package com.tesis.nsframework.config.loader;

import com.tesis.nsframework.config.binding.ActionBinding;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBindingRegistry {
    private final Map<String, ActionBinding> bindings = new ConcurrentHashMap<>();

    public void register(ActionBinding binding) {
        bindings.put(binding.action(), binding);
    }

    public Optional<ActionBinding> findByAction(String action) {
        return Optional.ofNullable(bindings.get(action));
    }
}
