package com.tesis.nsframework.config.binding;

import java.util.List;

public record ActionBindingsFile(List<ActionBinding> bindings) {
    public ActionBindingsFile {
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
    }
}
