package com.obracerta.item.dto;

import com.obracerta.item.domain.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(
    Long id,
    Long environmentId,
    String description,
    BigDecimal quantity,
    String unit,
    BigDecimal unitPrice,
    ItemType type,
    BigDecimal totalBudgeted,
    BigDecimal totalActual,
    boolean completed,
    boolean delayed,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
