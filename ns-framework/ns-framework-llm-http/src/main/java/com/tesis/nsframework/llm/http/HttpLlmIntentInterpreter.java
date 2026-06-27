package com.tesis.nsframework.llm.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsframework.core.exception.ExternalServiceException;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.port.IntentInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.LocalDate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Interprete generico basado en HTTP.
 * Asume que el endpoint LLM aguas arriba recibe un payload JSON con el prompt
 * y devuelve un cuerpo JSON con un objeto directo o un campo de texto con contenido JSON.
 */
public class HttpLlmIntentInterpreter implements IntentInterpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLlmIntentInterpreter.class);
    private static final String DEFAULT_SYSTEM_PROMPT =
            "Return only valid JSON with fields: intent, entities, constraints, confidence. Do not add markdown fences.";
    private static final String CITY_CATALOG_TOKEN = "{{CITY_CATALOG}}";
    private static final String ATTRACTION_CATALOG_TOKEN = "{{ATTRACTION_CATALOG}}";
    private static final String CATALOG_BASE_URL_OPTION = "catalog-base-url";
    private static final String CATALOG_CITIES_PATH_OPTION = "catalog-cities-path";
    private static final String CATALOG_ATTRACTIONS_PATH_OPTION = "catalog-attractions-path";
    private static final String LLM_SERVICE_NAME = "servicio LLM/Ollama";
    private static final String PROMPT_CATALOG_SERVICE_NAME = "servicio de catalogo para el prompt del LLM";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;
    private final Duration timeout;
    private final Map<String, Object> requestOptions;

    public HttpLlmIntentInterpreter(ObjectMapper objectMapper, URI endpoint, String apiKey, Duration timeout) {
        this(objectMapper, endpoint, apiKey, timeout, Map.of());
    }

    public HttpLlmIntentInterpreter(ObjectMapper objectMapper,
                                    URI endpoint,
                                    String apiKey,
                                    Duration timeout,
                                    Map<String, Object> requestOptions) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        this.requestOptions = requestOptions == null ? Map.of() : Map.copyOf(requestOptions);
    }

    @Override
    public InterpretationResult interpret(String userInput, DomainMetadata domainMetadata) {
        try {
            String prompt = buildPrompt(userInput, domainMetadata);

            Map<String, Object> payload = new LinkedHashMap<>(requestOptions);
            payload.put("system", renderSystemPrompt(payload));
            payload.put("prompt", prompt);
            payload.putIfAbsent("format", "json");
            LOGGER.debug("PAYLOAD: {}", payload);

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
                throw classifyHttpStatus(LLM_SERVICE_NAME,
                        response.statusCode(),
                        "La llamada al servicio LLM/Ollama devolvio status " + response.statusCode() + " en " + endpoint,
                        null);
            }

            LOGGER.debug("RESPUESTA_HTTP_LLM: {}", response.body());

            return parseInterpretation(response.body());
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            LOGGER.error("Tiempo de espera agotado en la solicitud al LLM. endpoint={}, timeoutSeconds={}", endpoint, timeout.toSeconds(), ex);
            throw ExternalServiceException.timeout(LLM_SERVICE_NAME,
                    "La solicitud al servicio LLM/Ollama excedio el tiempo de espera de " + timeout + " al invocar " + endpoint,
                    ex);
        } catch (ConnectException ex) {
            LOGGER.error("No se pudo conectar al endpoint LLM. endpoint={}", endpoint, ex);
            throw ExternalServiceException.unavailable(LLM_SERVICE_NAME,
                    "No se pudo conectar al servicio LLM/Ollama en " + endpoint,
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("Error al interpretar la intencion", ex);
            throw new FrameworkException("No se pudo interpretar la entrada del usuario", ex);
        } catch (IOException ex) {
            LOGGER.error("Error al interpretar la intencion", ex);
            throw ExternalServiceException.badGateway(LLM_SERVICE_NAME,
                    "El servicio LLM/Ollama devolvio una respuesta invalida o no pudo procesarse",
                    ex);
        }
    }

    private String buildPrompt(String userInput, DomainMetadata domainMetadata) {
        return "You are an intent extraction component for a neuro-symbolic framework. " +
                "Today's date (ISO-8601) is " + LocalDate.now() + ". " +
                "If the user provides day and month but no year, infer the nearest future calendar date from today. " +
                "Return only valid JSON with fields: intent, entities, constraints, confidence. " +
                "Supported intents: " + String.join(", ", domainMetadata.supportedIntents()) + ". " +
                "Map the user request into the closest supported intent. " +
                "User input: \"" + userInput + "\"";
    }

    private String renderSystemPrompt(Map<String, Object> payload) throws IOException, InterruptedException {
        String template = selectedSystemPrompt(payload);
        String catalogBaseUrl = stringOption(payload, CATALOG_BASE_URL_OPTION, "DEMO_TRAVEL_CATALOG_BASE_URL", "http://localhost:8085");
        String citiesPath = stringOption(payload, CATALOG_CITIES_PATH_OPTION, "DEMO_TRAVEL_CATALOG_CITIES_PATH", "/ciudades");
        String attractionsPath = stringOption(payload, CATALOG_ATTRACTIONS_PATH_OPTION, "DEMO_TRAVEL_CATALOG_ATTRACTIONS_PATH", "/atractivos");

        JsonNode cityRoot = fetchCatalogArray(catalogBaseUrl, citiesPath);
        JsonNode attractionRoot = fetchCatalogArray(catalogBaseUrl, attractionsPath);

        String cityCatalog = formatCatalogListing(cityRoot, "nombre", "id");
        String attractionCatalog = formatAttractionCatalogListing(attractionRoot, cityRoot, "nombre", "id");

        return template
                .replace(CITY_CATALOG_TOKEN, cityCatalog)
                .replace(ATTRACTION_CATALOG_TOKEN, attractionCatalog);
    }

    private JsonNode fetchCatalogArray(String baseUrl, String path) throws IOException, InterruptedException {
        String url = joinUrl(baseUrl, path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw classifyHttpStatus(PROMPT_CATALOG_SERVICE_NAME,
                        response.statusCode(),
                        "La consulta al catalogo del prompt devolvio status " + response.statusCode() + " para " + url,
                        null);
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                throw ExternalServiceException.badGateway(PROMPT_CATALOG_SERVICE_NAME,
                        "La respuesta del catalogo del prompt debe ser un arreglo JSON en " + url,
                        null);
            }

            return root;
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw ExternalServiceException.timeout(PROMPT_CATALOG_SERVICE_NAME,
                    "La consulta al catalogo del prompt excedio el tiempo de espera para " + url,
                    ex);
        } catch (ConnectException ex) {
            throw ExternalServiceException.unavailable(PROMPT_CATALOG_SERVICE_NAME,
                    "No se pudo conectar al catalogo del prompt en " + url,
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } catch (IOException ex) {
            throw ExternalServiceException.badGateway(PROMPT_CATALOG_SERVICE_NAME,
                    "No se pudo procesar la respuesta del catalogo del prompt en " + url,
                    ex);
        }
    }

    private String formatCatalogListing(JsonNode root, String labelField, String idField) {
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : root) {
            String id = textValue(item, idField, "id");
            String label = textValue(item, labelField, "nombre", "name");
            if (id == null || label == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(label).append(" -> ").append(id);
        }

        return builder.length() == 0 ? "- (no catalog entries)" : builder.toString();
    }

    private String formatAttractionCatalogListing(JsonNode attractionRoot,
                                                  JsonNode cityRoot,
                                                  String labelField,
                                                  String idField) {
        Map<String, String> cityNameById = new LinkedHashMap<>();
        for (JsonNode cityItem : cityRoot) {
            String cityId = textValue(cityItem, "id");
            String cityName = textValue(cityItem, "nombre", "name");
            if (cityId != null && cityName != null) {
                cityNameById.put(cityId, cityName);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : attractionRoot) {
            String id = textValue(item, idField, "id");
            String label = textValue(item, labelField, "nombre", "name");
            if (id == null || label == null) {
                continue;
            }
            String cityId = textValue(item, "ciudadId", "cityId");
            String cityName = cityId == null ? null : cityNameById.get(cityId);

            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(label).append(" -> ").append(id);
            if (cityId != null) {
                builder.append(" (ciudad: ");
                if (cityName != null) {
                    builder.append(cityName).append(" / ");
                }
                builder.append(cityId).append(')');
            }
        }

        return builder.length() == 0 ? "- (no catalog entries)" : builder.toString();
    }

    private String selectedSystemPrompt(Map<String, Object> payload) {
        return String.valueOf(payload.getOrDefault("system", env("PROXY_SYSTEM_PROMPT", DEFAULT_SYSTEM_PROMPT)));
    }

    private String stringOption(Map<String, Object> payload, String key, String envName, String defaultValue) {
        Object value = payload.get(key);
        if (value != null && !String.valueOf(value).isBlank()) {
            return String.valueOf(value).trim();
        }
        return env(envName, defaultValue);
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && field.isTextual() && !field.asText().isBlank()) {
                return field.asText().trim();
            }
        }
        return null;
    }

    private String joinUrl(String baseUrl, String path) {
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private ExternalServiceException classifyHttpStatus(String serviceName, int statusCode, String message, Throwable cause) {
        if (statusCode == 408 || statusCode == 504) {
            return ExternalServiceException.timeout(serviceName, message, cause);
        }
        if (statusCode >= 500) {
            return ExternalServiceException.unavailable(serviceName, message, cause);
        }
        return ExternalServiceException.badGateway(serviceName, message, cause);
    }

    private InterpretationResult parseInterpretation(String rawBody) throws IOException {
        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode effectiveNode = root;
        if (root.has("text") && root.get("text").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("text").asText());
        } else if (root.has("content") && root.get("content").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("content").asText());
        } else if (root.has("response") && root.get("response").isTextual()) {
            effectiveNode = objectMapper.readTree(root.get("response").asText());
        }

        LOGGER.debug("RESPUESTA_JSON_LLM: {}", effectiveNode.toString());

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
