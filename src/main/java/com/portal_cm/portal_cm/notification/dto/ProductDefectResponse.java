package com.portal_cm.portal_cm.notification.dto;

import com.portal_cm.portal_cm.notification.enums.DefectKind;

import java.time.LocalDate;

public record ProductDefectResponse(
        DefectKind kind,
        String productName,
        String manufacturer,
        String batch,
        LocalDate expiryDate,
        String equipment,
        String serialNumber,
        LocalDate lastPreventiveMaintenance,
        String assetNumber
) {
}
