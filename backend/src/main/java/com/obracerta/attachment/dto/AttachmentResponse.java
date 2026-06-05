package com.obracerta.attachment.dto;

import java.time.LocalDateTime;

public record AttachmentResponse(
    Long id,
    Long projectId,
    String fileName,
    String contentType,
    Long fileSize,
    LocalDateTime createdAt
) {}
