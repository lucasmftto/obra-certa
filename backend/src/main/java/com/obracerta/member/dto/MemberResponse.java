package com.obracerta.member.dto;

import com.obracerta.member.domain.MemberRole;

public record MemberResponse(
    Long userId,
    String name,
    String email,
    MemberRole role
) {}
