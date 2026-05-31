package com.obracerta.environment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record EnvironmentResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    Integer sortOrder,
    BigDecimal completionPercentage,
    @JsonProperty("hasDelayedItems") boolean hasDelayedItems
) {}
