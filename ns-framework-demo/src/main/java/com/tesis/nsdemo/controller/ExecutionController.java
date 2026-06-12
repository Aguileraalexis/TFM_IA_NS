package com.tesis.nsdemo.controller;

import com.tesis.nsdemo.dto.ExecuteRequest;
import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.port.ExecutionOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Travel Planner", description = "Planificación automática de viajes mediante NS Framework y PDDL")
public class ExecutionController {
    private final ExecutionOrchestrator orchestrator;
    private final TravelDomainPddlGenerator domainPddlGenerator;

    public ExecutionController(ExecutionOrchestrator orchestrator, TravelDomainPddlGenerator domainPddlGenerator) {
        this.orchestrator = orchestrator;
        this.domainPddlGenerator = domainPddlGenerator;
    }

    @Operation(
            summary = "Ejecutar plan de viaje",
            description = "Recibe un objetivo en lenguaje natural, lo convierte a problema PDDL, " +
                    "invoca el planificador y ejecuta las acciones resultantes (reserva de vuelo, hotel y atractivos).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Plan ejecutado con éxito",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ExecutionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Petición inválida")
            }
    )
    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> execute(@Valid @RequestBody ExecuteRequest request) {
        return ResponseEntity.ok(orchestrator.run(request.input()));
    }

    @Operation(
            summary = "Obtener dominio PDDL",
            description = "Devuelve el dominio PDDL generado dinámicamente para el planificador de viajes.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dominio PDDL en texto plano",
                            content = @Content(mediaType = "text/plain"))
            }
    )
    @GetMapping(value = "/domain", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> domain() {
        return ResponseEntity.ok(domainPddlGenerator.generate());
    }
}
