package com.obracerta.environment.controller;

import com.obracerta.environment.dto.EnvironmentRequest;
import com.obracerta.environment.dto.EnvironmentResponse;
import com.obracerta.environment.service.EnvironmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService service;

    @GetMapping("/api/v1/projects/{projectId}/environments")
    public ResponseEntity<List<EnvironmentResponse>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping("/api/v1/projects/{projectId}/environments")
    public ResponseEntity<EnvironmentResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody EnvironmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(projectId, request));
    }

    @GetMapping("/api/v1/environments/{id}")
    public ResponseEntity<EnvironmentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/api/v1/environments/{id}")
    public ResponseEntity<EnvironmentResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody EnvironmentRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/api/v1/environments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
