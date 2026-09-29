package com.portal_cm.portal_cm.notification.storage;

public record StoredFile(
        String relativePath,
        String originalName,
        String contentType,
        long size
) {
}
