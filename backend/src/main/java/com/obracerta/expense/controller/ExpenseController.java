package com.obracerta.expense.controller;

import com.obracerta.expense.dto.ExpenseRequest;
import com.obracerta.expense.dto.ExpenseResponse;
import com.obracerta.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService service;

    @GetMapping("/api/v1/items/{itemId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> list(@PathVariable Long itemId) {
        return ResponseEntity.ok(service.list(itemId));
    }

    @PostMapping("/api/v1/items/{itemId}/expenses")
    public ResponseEntity<ExpenseResponse> create(
            @PathVariable Long itemId,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(itemId, request));
    }

    @GetMapping("/api/v1/expenses/{id}")
    public ResponseEntity<ExpenseResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/api/v1/expenses/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/api/v1/expenses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
