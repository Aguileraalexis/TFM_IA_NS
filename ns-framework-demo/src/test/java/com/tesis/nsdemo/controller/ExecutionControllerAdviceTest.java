package com.tesis.nsdemo.controller;

import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.exception.ExternalServiceException;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.ExecutionOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExecutionControllerAdviceTest {

    private ExecutionOrchestrator orchestrator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orchestrator = mock(ExecutionOrchestrator.class);
        TravelDomainPddlGenerator generator = mock(TravelDomainPddlGenerator.class);
        ExecutionController controller = new ExecutionController(orchestrator, generator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ExecutionControllerAdvice())
                .build();
    }

    @Test
    void shouldReturn503WhenExternalServiceIsUnavailable() throws Exception {
        when(orchestrator.run(anyString())).thenThrow(
                ExternalServiceException.unavailable("servicio mock de vuelos",
                        "No se pudo conectar con el servicio mock de vuelos al obtener la lista de vuelos",
                        new RuntimeException("Connection refused")));

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"input\":\"viajar de madrid a paris\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se pudo conectar con el servicio mock de vuelos al obtener la lista de vuelos"));
    }

    @Test
    void shouldReturn504WhenExternalServiceTimesOut() throws Exception {
        when(orchestrator.run(anyString())).thenThrow(
                ExternalServiceException.timeout("servicio LLM/Ollama",
                        "La solicitud al servicio LLM/Ollama excedio el tiempo de espera",
                        new RuntimeException("timed out")));

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"input\":\"viajar de madrid a paris\"}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La solicitud al servicio LLM/Ollama excedio el tiempo de espera"));
    }

    @Test
    void shouldReturn422ForFrameworkFailures() throws Exception {
        when(orchestrator.run(anyString())).thenThrow(new FrameworkException("No se pudo inferir la ciudad de origen desde el prompt"));

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"input\":\"quiero viajar\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se pudo inferir la ciudad de origen desde el prompt"));
    }

    @Test
    void shouldExposeSuccessAndFailurePerExecutedActionInJson() throws Exception {
        when(orchestrator.run(anyString())).thenReturn(ExecutionResult.success(
                "Viaje completado exitosamente",
                java.util.List.of(
                        new ExecutionResult.ExecutedAction("book-flight(traveler_1, madr, pari)", false),
                        new ExecutionResult.ExecutedAction("book-flight(traveler_1, pari, logr)", true)
                ),
                1,
                new SymbolicState()
        ));

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"input\":\"viajar de madrid a logrono\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executedActions[0].invocation").value("book-flight(traveler_1, madr, pari)"))
                .andExpect(jsonPath("$.executedActions[0].success").value(false))
                .andExpect(jsonPath("$.executedActions[1].invocation").value("book-flight(traveler_1, pari, logr)"))
                .andExpect(jsonPath("$.executedActions[1].success").value(true))
                .andExpect(jsonPath("$.replans").value(1));
    }
}
