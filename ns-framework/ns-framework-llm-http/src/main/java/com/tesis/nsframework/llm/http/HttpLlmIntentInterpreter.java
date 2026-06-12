package com.tesis.nsframework.llm.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.port.IntentInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic HTTP-based interpreter. It assumes the upstream LLM endpoint accepts a JSON payload with a prompt
 * and returns a JSON body containing either a direct JSON object or a text field with JSON content.
 */
public class HttpLlmIntentInterpreter implements IntentInterpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLlmIntentInterpreter.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;
    private final Duration timeout;

    public HttpLlmIntentInterpreter(ObjectMapper objectMapper, URI endpoint, String apiKey, Duration timeout) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    @Override
    public InterpretationResult interpret(String userInput, DomainMetadata domainMetadata) {
        try {
            String prompt = buildPrompt(userInput, domainMetadata);
            Map<String, Object> payload = Map.of(
                    "prompt", prompt,
                    "format", "json"
            );

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FrameworkException("LLM call failed with status " + response.statusCode());
            }

            return parseInterpretation(response.body());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("Failed to interpret intent", ex);
            throw new FrameworkException("Failed to interpret user input", ex);
        }
    }

    private String buildPrompt(String userInput, DomainMetadata domainMetadata) {
        return "You are an intent extraction component for a neuro-symbolic framework. " +
                "Return only valid JSON with fields: intent, entities, constraints, confidence. " +
                "Supported intents: " + String.join(", ", domainMetadata.supportedIntents()) + ". " +
                "Map the user request into the closest supported intent. " +
                "User input: \"" + userInput + "\"";
    }

    private InterpretationResult parseInterpretation(String rawBody) throws IOException {
        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode effectiveNode = root;
        if (root.has("text") && root.get("text").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("text").asText());
        } else if (root.has("content") && root.get("content").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("content").asText());
        }

        String intent = effectiveNode.path("intent").asText(null);
        Double confidence = effectiveNode.has("confidence") ? effectiveNode.path("confidence").asDouble() : null;
        Map<String, Object> entities = effectiveNode.has("entities")
                ? objectMapper.convertValue(effectiveNode.get("entities"), new TypeReference<HashMap<String, Object>>() {})
                : Map.of();
        Map<String, Object> constraints = effectiveNode.has("constraints")
                ? objectMapper.convertValue(effectiveNode.get("constraints"), new TypeReference<HashMap<String, Object>>() {})
                : Map.of();

        return new InterpretationResult(intent, entities, constraints, confidence, rawBody);
    }
}
