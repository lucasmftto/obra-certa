package com.obracerta.project.dto;

import com.obracerta.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
    @NotNull(message = "Status é obrigatório")
    ProjectStatus status
) {}
