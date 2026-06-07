package com.obracerta.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    Long ownerId,
    String name,
    ProjectType type,
    String address,
    String description,
    ProjectStatus status,
    BigDecimal totalBudget,
    BigDecimal totalActual,
    BigDecimal remainingBalance,
    BigDecimal actualPercentage,
    BigDecimal completionPercentage,
    boolean overBudget,
    @JsonProperty("hasDelayedItems") boolean hasDelayedItems,
    int roomCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
