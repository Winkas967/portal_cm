package com.portal_cm.portal_cm.notification.enums;

import jakarta.persistence.Converter;

public enum MedicationErrorStage {
    PRESCRICAO,
    DISPENSACAO,
    ADMINISTRACAO;

    @Converter
    public static class DbConverter extends LowercaseEnumConverter<MedicationErrorStage> {
        public DbConverter() {
            super(MedicationErrorStage.class);
        }
    }
}
