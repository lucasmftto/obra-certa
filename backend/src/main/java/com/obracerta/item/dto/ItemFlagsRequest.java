package com.obracerta.item.dto;

public record ItemFlagsRequest(
    Boolean completed,
    Boolean delayed
) {}
