package com.obracerta.project.dto;

import com.obracerta.project.domain.ProjectType;
import jakarta.validation.constraints.*;

public record ProjectRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 200, message = "Nome deve ter entre 3 e 200 caracteres")
    String name,

    @NotNull(message = "Tipo é obrigatório")
    ProjectType type,

    @Size(max = 300, message = "Endereço deve ter no máximo 300 caracteres")
    String address,

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    String description
) {}
