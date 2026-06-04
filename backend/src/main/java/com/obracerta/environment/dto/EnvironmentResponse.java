package com.obracerta.environment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record EnvironmentResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    BigDecimal completionPercentage,
    @JsonProperty("hasDelayedItems") boolean hasDelayedItems
) {}
