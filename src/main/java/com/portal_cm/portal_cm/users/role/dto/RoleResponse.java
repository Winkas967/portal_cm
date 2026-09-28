package com.portal_cm.portal_cm.users.role.dto;

public record RoleResponse(
        Integer id,
        String name,
        String role,
        boolean isActive,
        long userCount
) {
}
