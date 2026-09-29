package com.portal_cm.portal_cm.notification.enums;

import jakarta.persistence.Converter;

public enum PhlebitisType {
    QUIMICA,
    MECANICA,
    INFECCIOSA;

    @Converter
    public static class DbConverter extends LowercaseEnumConverter<PhlebitisType> {
        public DbConverter() {
            super(PhlebitisType.class);
        }
    }
}
