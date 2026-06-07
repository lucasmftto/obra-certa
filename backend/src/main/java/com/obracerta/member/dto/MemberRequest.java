package com.obracerta.member.dto;

import com.obracerta.member.domain.MemberRole;
import jakarta.validation.constraints.*;

public record MemberRequest(
    @NotBlank @Email String email,
    @NotNull MemberRole role
) {}
