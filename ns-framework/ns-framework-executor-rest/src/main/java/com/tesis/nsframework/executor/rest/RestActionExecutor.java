package com.tesis.nsframework.executor.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsframework.config.binding.ActionBinding;
import com.tesis.nsframework.config.loader.ActionBindingRegistry;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.ExecutionContext;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.port.ActionExecutor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RestActionExecutor implements ActionExecutor {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final ActionBindingRegistry bindingRegistry;
    private final SuccessConditionEvaluator successConditionEvaluator;
    private final TemplateInterpolator templateInterpolator;
    private final String baseUrl;
    private final Duration timeout;

    public RestActionExecutor(ObjectMapper objectMapper,
                              ActionBindingRegistry bindingRegistry,
                              String baseUrl,
                              Duration timeout) {
        this.objectMapper = objectMapper;
        this.bindingRegistry = bindingRegistry;
        this.baseUrl = baseUrl;
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
        this.successConditionEvaluator = new SuccessConditionEvaluator();
        this.templateInterpolator = new TemplateInterpolator();
    }

    @Override
    public ActionOutcome execute(PlannedAction action, ExecutionContext context) {
        ActionBinding binding = bindingRegistry.findByAction(action.name())
                .orElseThrow(() -> new FrameworkException("No binding configured for action " + action.name()));
        try {
            Map<String, Object> requestBody = templateInterpolator.interpolateMap(binding.body(), context);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + binding.endpoint()))
                    .timeout(timeout);

            binding.headers().forEach(requestBuilder::header);
            String body = objectMapper.writeValueAsString(requestBody);

            String method = binding.method().toUpperCase();
            switch (method) {
                case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                case "DELETE" -> requestBuilder.DELETE();
                default -> requestBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            boolean success = successConditionEvaluator.isSuccessful(binding.successCondition(), response.statusCode());
            Map<String, Object> payload = parsePayload(response.body());
            Map<String, String> observedEffects = new HashMap<>(binding.effectsMapping());
            return success
                    ? ActionOutcome.success(response.statusCode(), "Action executed successfully", payload, observedEffects)
                    : ActionOutcome.failure(response.statusCode(), "Action execution failed", payload);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FrameworkException("Failed to execute REST action " + action.name(), ex);
        }
    }

    private Map<String, Object> parsePayload(String rawBody) {
        try {
            if (rawBody == null || rawBody.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (IOException ex) {
            return Map.of("rawBody", rawBody);
        }
    }
}
