package com.portal_cm.portal_cm.notification.dto;

import com.portal_cm.portal_cm.notification.catalog.dto.IncidentTypeResponse;
import com.portal_cm.portal_cm.notification.enums.MedicationErrorStage;
import com.portal_cm.portal_cm.notification.enums.PhlebitisType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record NotificationDetailResponse(
        Integer id,
        OffsetDateTime notificationDate,
        OffsetDateTime eventDate,
        NamedRef sector,
        NamedRef notifier,
        List<IncidentTypeResponse> incidentTypes,
        MedicationErrorStage medicationErrorStage,
        PhlebitisType phlebitisType,
        String otherIncidentDescription,
        ProductDefectResponse productDefect,
        String nonconformity,
        String patientName,
        LocalDate patientBirthDate,
        String patientColor,
        String visitReason,
        String medicalRecordNumber,
        String attendanceNumber,
        String wardBed,
        String eventDescription,
        String immediateActions,
        List<AttachmentResponse> attachments
) {
}
