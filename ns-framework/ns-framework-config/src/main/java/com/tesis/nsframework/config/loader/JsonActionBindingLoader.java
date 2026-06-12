package com.tesis.nsframework.config.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsframework.config.binding.ActionBindingsFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonActionBindingLoader {
    private final ObjectMapper objectMapper;

    public JsonActionBindingLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ActionBindingRegistry load(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            ActionBindingsFile file = objectMapper.readValue(inputStream, ActionBindingsFile.class);
            ActionBindingRegistry registry = new ActionBindingRegistry();
            file.bindings().forEach(registry::register);
            return registry;
        }
    }
}
