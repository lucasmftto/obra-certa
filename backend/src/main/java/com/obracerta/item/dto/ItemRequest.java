package com.obracerta.item.dto;

import com.obracerta.item.domain.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequest(
    @NotBlank @Size(max = 300) String description,
    @NotNull @DecimalMin("0.001") BigDecimal quantity,
    @NotBlank @Size(max = 10) String unit,
    @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
    @NotNull ItemType type
) {}
