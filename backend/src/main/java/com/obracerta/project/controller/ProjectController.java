package com.obracerta.project.controller;

import com.obracerta.auth.domain.User;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.project.dto.ProjectRequest;
import com.obracerta.project.dto.ProjectResponse;
import com.obracerta.project.dto.ProjectSummaryResponse;
import com.obracerta.project.dto.StatusUpdateRequest;
import com.obracerta.project.service.ProjectService;
import com.obracerta.project.service.ProjectSummaryService;
import com.obracerta.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;
    private final ProjectSummaryService summaryService;

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> list(
        @RequestParam(required = false) ProjectStatus status,
        @RequestParam(required = false) ProjectType type,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @AuthenticationPrincipal User currentUser
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        return ResponseEntity.ok(PageResponse.of(service.list(status, type, search, currentUser.getId(), pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> findById(
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.findById(id, currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
        @Valid @RequestBody ProjectRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, currentUser.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody ProjectRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.update(id, request, currentUser.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody StatusUpdateRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request.status(), currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        service.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ProjectSummaryResponse> summary(@PathVariable Long id) {
        return ResponseEntity.ok(summaryService.summary(id));
    }
}
