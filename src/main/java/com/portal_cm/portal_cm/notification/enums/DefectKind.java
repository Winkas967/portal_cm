package com.portal_cm.portal_cm.notification.enums;

import jakarta.persistence.Converter;

public enum DefectKind {
    MEDICAMENTO,
    EQUIPAMENTO;

    @Converter
    public static class DbConverter extends LowercaseEnumConverter<DefectKind> {
        public DbConverter() {
            super(DefectKind.class);
        }
    }
}
