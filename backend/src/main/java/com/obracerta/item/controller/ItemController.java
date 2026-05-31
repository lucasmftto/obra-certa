package com.obracerta.item.controller;

import com.obracerta.item.dto.ItemFlagsRequest;
import com.obracerta.item.dto.ItemRequest;
import com.obracerta.item.dto.ItemResponse;
import com.obracerta.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService service;

    @GetMapping("/api/v1/environments/{environmentId}/items")
    public ResponseEntity<List<ItemResponse>> list(@PathVariable Long environmentId) {
        return ResponseEntity.ok(service.list(environmentId));
    }

    @PostMapping("/api/v1/environments/{environmentId}/items")
    public ResponseEntity<ItemResponse> create(
            @PathVariable Long environmentId,
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(environmentId, request));
    }

    @GetMapping("/api/v1/items/{id}")
    public ResponseEntity<ItemResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/api/v1/items/{id}")
    public ResponseEntity<ItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/api/v1/items/{id}/flags")
    public ResponseEntity<ItemResponse> updateFlags(
            @PathVariable Long id,
            @RequestBody ItemFlagsRequest request) {
        return ResponseEntity.ok(service.updateFlags(id, request));
    }

    @DeleteMapping("/api/v1/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
