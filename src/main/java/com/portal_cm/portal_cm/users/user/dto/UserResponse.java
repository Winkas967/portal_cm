package com.portal_cm.portal_cm.users.user.dto;

public record UserResponse(
        Integer id,
        String name,
        boolean isActive,
        Integer roleId,
        String roleName,
        String roleValue
) {
}
