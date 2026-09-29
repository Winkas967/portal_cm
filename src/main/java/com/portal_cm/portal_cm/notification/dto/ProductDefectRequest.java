package com.portal_cm.portal_cm.notification.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProductDefectRequest(
        @Size(max = 150, message = "O nome do produto deve ter no máximo 150 caracteres") String productName,
        @Size(max = 150, message = "O fabricante deve ter no máximo 150 caracteres") String manufacturer,
        @Size(max = 50, message = "O lote deve ter no máximo 50 caracteres") String batch,
        LocalDate expiryDate,
        @Size(max = 150, message = "O equipamento deve ter no máximo 150 caracteres") String equipment,
        @Size(max = 80, message = "O número de série deve ter no máximo 80 caracteres") String serialNumber,
        LocalDate lastPreventiveMaintenance,
        @Size(max = 50, message = "O patrimônio deve ter no máximo 50 caracteres") String assetNumber
) {
}