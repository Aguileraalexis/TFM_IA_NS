package com.tesis.nsframework.llm.http;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tesis.nsframework.core.exception.ExternalServiceException;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class HttpLlmPlannerTest {
    private HttpServer server;
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }
    @Test
    void shouldBuildPromptAndParsePlanFromResponseField() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/plan", exchange -> respondPlan(exchange, requestBody, responseWithWrappedPlan()));
        server.start();
        HttpLlmPlanner planner = new HttpLlmPlanner(
                new ObjectMapper(),
                URI.create("http://localhost:" + server.getAddress().getPort() + "/plan"),
                null,
                Duration.ofSeconds(5),
                Map.of("model", "llama3.1", "stream", false)
        );
        PlanResult result = planner.plan(domainPddl(), planningProblem(), PlannerOptions.defaults());
        assertTrue(result.success());
        assertEquals(2, result.actions().size());
        assertEquals("book-flight", result.actions().getFirst().name());
        assertEquals(List.of("traveler_1", "MADR", "LOGR"), result.actions().getFirst().arguments());
        assertTrue(result.rawPlan().contains("book-hotel traveler_1 HT020 LOGR"));
        String payload = requestBody.get();
        assertTrue(payload.contains("\"model\":\"llama3.1\""));
        assertTrue(payload.contains("\"format\":\"json\""));
        assertTrue(payload.contains("traveler_1"), payload);
        assertTrue(payload.contains("selectedHotelByCity"), payload);
        assertTrue(payload.contains("(flight-available MADR LOGR)"), payload);
    }
    @Test
    void shouldReturnFailurePlanWhenLlmDeclaresNoPlan() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/plan", exchange -> respondPlan(exchange, new AtomicReference<>(),
                "{\"success\":false,\"message\":\"No existe una ruta valida\",\"actions\":[]}"));
        server.start();
        HttpLlmPlanner planner = new HttpLlmPlanner(
                new ObjectMapper(),
                URI.create("http://localhost:" + server.getAddress().getPort() + "/plan"),
                null,
                Duration.ofSeconds(5),
                Map.of()
        );
        PlanResult result = planner.plan(domainPddl(), planningProblem(), PlannerOptions.defaults());
        assertFalse(result.success());
        assertEquals("No existe una ruta valida", result.message());
        assertTrue(result.actions().isEmpty());
    }
    @Test
    void shouldReturnExplicitMessageWhenPlannerEndpointIsUnreachable() throws Exception {
        int closedPort = findClosedPort();
        URI unreachableEndpoint = URI.create("http://localhost:" + closedPort + "/plan");
        HttpLlmPlanner planner = new HttpLlmPlanner(
                new ObjectMapper(),
                unreachableEndpoint,
                null,
                Duration.ofSeconds(2),
                Map.of()
        );
        FrameworkException exception = assertThrows(
                FrameworkException.class,
                () -> planner.plan(domainPddl(), planningProblem(), PlannerOptions.defaults())
        );
        assertInstanceOf(ExternalServiceException.class, exception);
        assertTrue(exception.getMessage().contains("No se pudo conectar al planner LLM en " + unreachableEndpoint));
        assertInstanceOf(java.net.ConnectException.class, exception.getCause());
    }
    private static PlanningProblem planningProblem() {
        return new PlanningProblem(
                "travel-problem-1",
                "travel-domain",
                Set.of("traveler_1 - traveler"),
                Set.of("(at traveler_1 MADR)", "(flight-available MADR LOGR)", "(hotel-in-city HT020 LOGR)"),
                "(and (visited-city LOGR) (hotel-booked traveler_1 HT020 LOGR))",
                Map.of(
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1",
                        "originCityId", "MADR",
                        "targetCityIds", List.of("LOGR"),
                        "selectedHotelByCity", Map.of("LOGR", "HT020"),
                        "attractionsByCity", Map.of("LOGR", List.of()),
                        "preferredAirlineByRoute", Map.of("MADR->LOGR", "AL01"),
                        "travelDate", "2026-07-10"
                )
        );
    }
    private static String domainPddl() {
        return "(define (domain travel-demo) (:requirements :typing) (:predicates (at ?t - traveler ?c - city)))";
    }
    private static String responseWithWrappedPlan() throws IOException {
        String innerPlan = new ObjectMapper().writeValueAsString(Map.of(
                "success", true,
                "rawPlan", "(book-flight traveler_1 MADR LOGR)\n(book-hotel traveler_1 HT020 LOGR)",
                "actions", List.of(
                        Map.of("name", "book-flight", "arguments", List.of("traveler_1", "MADR", "LOGR")),
                        Map.of("name", "book-hotel", "arguments", List.of("traveler_1", "HT020", "LOGR"))
                )
        ));
        return new ObjectMapper().writeValueAsString(Map.of("response", innerPlan));
    }
    private static int findClosedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
    private static void respondPlan(HttpExchange exchange, AtomicReference<String> requestBody, String body) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            requestBody.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}