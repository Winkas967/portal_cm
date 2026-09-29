package com.portal_cm.portal_cm.notification.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record NotificationSummaryResponse(
        Integer id,
        OffsetDateTime notificationDate,
        OffsetDateTime eventDate,
        NamedRef sector,
        NamedRef notifier,
        String patientName,
        List<String> incidentTypes,
        int attachmentCount
) {
}
