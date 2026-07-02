package com.tesis.nsframework.llm.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsframework.core.exception.ExternalServiceException;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.port.Planner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Planner que delega la construccion del plan a un endpoint HTTP de LLM.
 * El modelo debe devolver JSON estricto con la secuencia de acciones.
 */
public class HttpLlmPlanner implements Planner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLlmPlanner.class);
    private static final String LLM_PLANNER_SERVICE_NAME = "servicio planner LLM";
    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are an action planner component for a travel neuro-symbolic framework.
            Return only valid JSON with fields: success, message, rawPlan, actions.
            actions must be an array of objects with fields: name, arguments.
            Allowed action names only:
            - book-flight with arguments [travelerSymbol, originCityId, destinationCityId]
            - book-hotel with arguments [travelerSymbol, hotelId, cityId]
            - visit-attraction with arguments [travelerSymbol, attractionId, cityId]
            Use only IDs that already exist in the provided problem metadata, objects, init facts, or goal.
            If no valid plan exists, return {"success":false,"message":"explicacion breve","actions":[]}.
            Do not wrap output in markdown code fences.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;
    private final Duration timeout;
    private final Map<String, Object> requestOptions;

    public HttpLlmPlanner(ObjectMapper objectMapper, URI endpoint, String apiKey, Duration timeout) {
        this(objectMapper, endpoint, apiKey, timeout, Map.of());
    }

    public HttpLlmPlanner(ObjectMapper objectMapper,
                          URI endpoint,
                          String apiKey,
                          Duration timeout,
                          Map<String, Object> requestOptions) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
        this.requestOptions = requestOptions == null ? Map.of() : Map.copyOf(requestOptions);
    }

    @Override
    public PlanResult plan(String domainPddl, PlanningProblem problem, PlannerOptions options) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>(requestOptions);
            payload.put("system", renderSystemPrompt(payload));
            payload.put("prompt", buildPrompt(domainPddl, problem, options));
            payload.putIfAbsent("format", "json");
            LOGGER.debug("PAYLOAD_PLANNER_LLM: {}", payload);

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
                throw classifyHttpStatus(response.statusCode(),
                        "La llamada al planner LLM devolvio status " + response.statusCode() + " en " + endpoint,
                        null);
            }

            LOGGER.debug("RESPUESTA_HTTP_PLANNER_LLM: {}", response.body());
            return parsePlanResult(response.body());
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            LOGGER.error("Tiempo de espera agotado en la solicitud al planner LLM. endpoint={}, timeoutSeconds={}", endpoint, timeout.toSeconds(), ex);
            throw ExternalServiceException.timeout(LLM_PLANNER_SERVICE_NAME,
                    "La solicitud al planner LLM excedio el tiempo de espera de " + timeout + " al invocar " + endpoint,
                    ex);
        } catch (ConnectException ex) {
            LOGGER.error("No se pudo conectar al endpoint del planner LLM. endpoint={}", endpoint, ex);
            throw ExternalServiceException.unavailable(LLM_PLANNER_SERVICE_NAME,
                    "No se pudo conectar al planner LLM en " + endpoint,
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FrameworkException("La solicitud al planner LLM fue interrumpida", ex);
        } catch (IOException ex) {
            ConnectException connectException = findCause(ex, ConnectException.class);
            if (connectException != null) {
                LOGGER.error("No se pudo conectar al endpoint del planner LLM. endpoint={}", endpoint, connectException);
                throw ExternalServiceException.unavailable(LLM_PLANNER_SERVICE_NAME,
                        "No se pudo conectar al planner LLM en " + endpoint,
                        connectException);
            }
            LOGGER.error("Error al procesar la respuesta del planner LLM", ex);
            throw ExternalServiceException.badGateway(LLM_PLANNER_SERVICE_NAME,
                    "El planner LLM devolvio una respuesta invalida o no pudo procesarse",
                    ex);
        }
    }

    private String buildPrompt(String domainPddl, PlanningProblem problem, PlannerOptions options) throws IOException {
        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("problemName", problem.problemName());
        promptData.put("domainName", problem.domainName());
        promptData.put("objects", new TreeSet<>(problem.objects()));
        promptData.put("initFacts", new TreeSet<>(problem.initFacts()));
        promptData.put("goalExpression", problem.goalExpression());
        promptData.put("metadata", problem.metadata());
        promptData.put("plannerTimeoutSeconds", options == null || options.timeout() == null ? null : options.timeout().toSeconds());
        promptData.put("plannerSearchStrategy", options == null ? null : options.strategy());

        return """
                Build a valid plan for the provided travel planning problem.
                Return only valid JSON with fields success, message, rawPlan, actions.
                Each item in actions must have fields name and arguments.
                Use the exact traveler symbol and IDs already present in the problem.
                Do not invent flights, hotels, attractions, or cities.
                Prefer the shortest valid plan that satisfies the goal.

                DOMAIN_PDDL:
                %s

                PLANNING_PROBLEM_JSON:
                %s
                """.formatted(domainPddl, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(promptData));
    }

    private String renderSystemPrompt(Map<String, Object> payload) {
        return String.valueOf(payload.getOrDefault("system", DEFAULT_SYSTEM_PROMPT));
    }

    private PlanResult parsePlanResult(String rawBody) throws IOException {
        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode effectiveNode = unwrapStructuredPayload(root);
        LOGGER.debug("RESPUESTA_JSON_PLANNER_LLM: {}", effectiveNode.toString());

        boolean success = !effectiveNode.has("success") || effectiveNode.path("success").asBoolean(true);
        String message = effectiveNode.path("message").asText("OK");
        if (!success) {
            return PlanResult.failure(message == null || message.isBlank()
                    ? "El planner LLM indico que no existe un plan valido"
                    : message);
        }

        JsonNode actionsNode = effectiveNode.get("actions");
        if (actionsNode == null || !actionsNode.isArray()) {
            throw ExternalServiceException.badGateway(LLM_PLANNER_SERVICE_NAME,
                    "La respuesta del planner LLM debe incluir un arreglo JSON 'actions'",
                    null);
        }

        List<PlannedAction> actions = new ArrayList<>();
        for (JsonNode actionNode : actionsNode) {
            String actionName = firstText(actionNode, "name", "action");
            if (actionName == null || actionName.isBlank()) {
                throw ExternalServiceException.badGateway(LLM_PLANNER_SERVICE_NAME,
                        "Cada accion del planner LLM debe incluir un nombre valido",
                        null);
            }

            JsonNode argumentsNode = actionNode.get("arguments");
            if (argumentsNode == null || !argumentsNode.isArray()) {
                argumentsNode = actionNode.get("args");
            }
            if (argumentsNode == null || !argumentsNode.isArray()) {
                throw ExternalServiceException.badGateway(LLM_PLANNER_SERVICE_NAME,
                        "Cada accion del planner LLM debe incluir un arreglo 'arguments'",
                        null);
            }

            List<String> arguments = new ArrayList<>();
            for (JsonNode argumentNode : argumentsNode) {
                arguments.add(argumentNode.asText());
            }
            actions.add(new PlannedAction(actionName, arguments, Map.of()));
        }

        if (actions.isEmpty()) {
            return PlanResult.failure(message == null || message.isBlank()
                    ? "El planner LLM no devolvio acciones"
                    : message);
        }

        String rawPlan = effectiveNode.has("rawPlan") && effectiveNode.get("rawPlan").isTextual()
                ? effectiveNode.get("rawPlan").asText()
                : renderFallbackRawPlan(actions);
        return PlanResult.success(actions, rawPlan);
    }

    private JsonNode unwrapStructuredPayload(JsonNode root) throws IOException {
        JsonNode effectiveNode = root;
        if (root.has("text") && root.get("text").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("text").asText());
        } else if (root.has("content") && root.get("content").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("content").asText());
        } else if (root.has("response") && root.get("response").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("response").asText());
        }
        return effectiveNode;
    }

    private String renderFallbackRawPlan(List<PlannedAction> actions) {
        StringBuilder builder = new StringBuilder();
        for (PlannedAction action : actions) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(action.asInvocation());
        }
        return builder.toString();
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && field.isTextual() && !field.asText().isBlank()) {
                return field.asText().trim();
            }
        }
        return null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private ExternalServiceException classifyHttpStatus(int statusCode, String message, Throwable cause) {
        if (statusCode == 408 || statusCode == 504) {
            return ExternalServiceException.timeout(LLM_PLANNER_SERVICE_NAME, message, cause);
        }
        if (statusCode >= 500) {
            return ExternalServiceException.unavailable(LLM_PLANNER_SERVICE_NAME, message, cause);
        }
        return ExternalServiceException.badGateway(LLM_PLANNER_SERVICE_NAME, message, cause);
    }
}



