package com.portal_cm.portal_cm.notification.dto;

import java.time.OffsetDateTime;

public record AttachmentResponse(
        Integer id,
        String originalName,
        String contentType,
        long sizeBytes,
        OffsetDateTime uploadedAt,
        String downloadUrl
) {
}
