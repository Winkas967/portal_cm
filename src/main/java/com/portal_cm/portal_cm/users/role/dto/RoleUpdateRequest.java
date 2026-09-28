package com.portal_cm.portal_cm.users.role.dto;

import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String name,

        @Size(max = 20, message = "A role deve ter no máximo 20 caracteres")
        String role
) {
}
