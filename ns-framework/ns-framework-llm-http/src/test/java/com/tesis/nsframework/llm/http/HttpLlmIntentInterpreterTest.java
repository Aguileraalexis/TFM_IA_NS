package com.tesis.nsframework.llm.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.InterpretationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpLlmIntentInterpreterTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldMergeRequestOptionsFetchRuntimeCatalogAndParseResponseField() throws Exception {
        LocalDate today = LocalDate.now();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ciudades", exchange -> respond(exchange, "[{\"id\":\"MADR\",\"nombre\":\"Madrid\"},{\"id\":\"LOGR\",\"nombre\":\"Logroño\"}]"));
        server.createContext("/atractivos", exchange -> respond(exchange, "[{\"id\":\"AT021\",\"ciudadId\":\"LOGR\",\"nombre\":\"Universidad de La Rioja\"}]"));
        server.createContext("/interpret", exchange -> respondInterpret(exchange, requestBody));
        server.start();

        Map<String, Object> requestOptions = new LinkedHashMap<>();
        requestOptions.put("model", "llama3.1");
        requestOptions.put("stream", false);
        requestOptions.put("catalog-base-url", "http://localhost:" + server.getAddress().getPort());
        requestOptions.put("catalog-cities-path", "/ciudades");
        requestOptions.put("catalog-attractions-path", "/atractivos");
        requestOptions.put("system", """
                Cities:
                {{CITY_CATALOG}}

                Attractions:
                {{ATTRACTION_CATALOG}}
                """);

        HttpLlmIntentInterpreter interpreter = new HttpLlmIntentInterpreter(
                new ObjectMapper(),
                URI.create("http://localhost:" + server.getAddress().getPort() + "/interpret"),
                null,
                Duration.ofSeconds(5),
                requestOptions
        );

        InterpretationResult result = interpreter.interpret(
                "Quiero viajar desde Madrid a Logrono",
                new DomainMetadata("travel", java.util.Set.of("plan_trip"), Map.of("plan_trip", "travel_goal"))
        );

        assertEquals("plan_trip", result.intent());
        assertEquals("MADR", result.entities().get("originCityId"));
        assertEquals("LOGR", result.entities().get("targetCityIds"));
        assertEquals(0.91, result.confidence());

        String payload = requestBody.get();
        assertTrue(payload.contains("\"model\":\"llama3.1\""));
        assertTrue(payload.contains("\"stream\":false"));
        assertTrue(payload.contains("\"format\":\"json\""));
        assertTrue(payload.contains("Today's date (ISO-8601) is " + today), payload);
        assertTrue(payload.contains("If the user provides day and month but no year"), payload);
        assertTrue(payload.contains("User input: "));
        assertFalse(payload.contains("{{CITY_CATALOG}}"), payload);
        assertFalse(payload.contains("{{ATTRACTION_CATALOG}}"), payload);
        assertTrue(payload.contains("Madrid"));
        assertTrue(payload.contains("Logroño"));
        assertTrue(payload.contains("Universidad de La Rioja"));
        assertTrue(payload.contains("MADR"));
        assertTrue(payload.contains("LOGR"));
        assertTrue(payload.contains("AT021"));
        assertTrue(payload.contains("(ciudad: Logroño / LOGR)"), payload);
        assertFalse(payload.contains("\"prompt\":null"));
    }

    @Test
    void shouldReturnExplicitMessageWhenLlmEndpointIsUnreachable() throws Exception {
        int closedPort = findClosedPort();
        URI unreachableEndpoint = URI.create("http://localhost:" + closedPort + "/interpret");

        HttpLlmIntentInterpreter interpreter = new HttpLlmIntentInterpreter(
                new ObjectMapper(),
                unreachableEndpoint,
                null,
                Duration.ofSeconds(2),
                Map.of()
        );

        FrameworkException exception = assertThrows(
                FrameworkException.class,
                () -> interpreter.interpret(
                        "Quiero viajar desde Madrid a Logrono",
                        new DomainMetadata("travel", java.util.Set.of("plan_trip"), Map.of("plan_trip", "travel_goal"))
                )
        );

        assertTrue(exception.getMessage().contains("No se pudo conectar a " + unreachableEndpoint));
        assertInstanceOf(java.net.ConnectException.class, exception.getCause());
    }

    private static int findClosedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private static void respondInterpret(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            requestBody.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }

        String body = "{\"response\":\"{\\\"intent\\\":\\\"plan_trip\\\",\\\"entities\\\":{\\\"originCityId\\\":\\\"MADR\\\",\\\"targetCityIds\\\":\\\"LOGR\\\"},\\\"constraints\\\":{},\\\"confidence\\\":0.91}\"}";
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}

