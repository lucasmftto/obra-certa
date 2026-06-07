package com.obracerta.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password
) {}
