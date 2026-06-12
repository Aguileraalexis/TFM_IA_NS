package com.tesis.nsdemo.controller;

import com.tesis.nsdemo.dto.ExecuteRequest;
import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.port.ExecutionOrchestrator;
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
public class ExecutionController {
    private final ExecutionOrchestrator orchestrator;
    private final TravelDomainPddlGenerator domainPddlGenerator;

    public ExecutionController(ExecutionOrchestrator orchestrator, TravelDomainPddlGenerator domainPddlGenerator) {
        this.orchestrator = orchestrator;
        this.domainPddlGenerator = domainPddlGenerator;
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> execute(@Valid @RequestBody ExecuteRequest request) {
        return ResponseEntity.ok(orchestrator.run(request.input()));
    }

    @GetMapping(value = "/domain", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> domain() {
        return ResponseEntity.ok(domainPddlGenerator.generate());
    }
}
