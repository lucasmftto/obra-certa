package com.obracerta.environment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EnvironmentRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    String name,

    String description,

    Integer sortOrder,

    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    BigDecimal completionPercentage
) {}
