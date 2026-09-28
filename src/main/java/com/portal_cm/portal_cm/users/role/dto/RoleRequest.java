package com.portal_cm.portal_cm.users.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(
        @NotBlank(message = "Preencha o nome da role")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String name,

        @NotBlank(message = "Preencha a role")
        @Size(max = 20, message = "A role deve ter no máximo 20 caracteres")
        String role
) {
}
