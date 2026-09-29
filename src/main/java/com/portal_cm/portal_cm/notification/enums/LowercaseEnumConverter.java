package com.portal_cm.portal_cm.notification.enums;

import jakarta.persistence.AttributeConverter;

import java.util.Locale;

public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected LowercaseEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : Enum.valueOf(type, dbValue.toUpperCase(Locale.ROOT));
    }
}
