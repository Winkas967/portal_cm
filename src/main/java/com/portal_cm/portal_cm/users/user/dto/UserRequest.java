package com.portal_cm.portal_cm.users.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Preencha o nome do usuário")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String name,

        @NotBlank(message = "Preencha a senha")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @NotNull(message = "Selecione a role do usuário")
        @Positive(message = "Role inválida")
        Integer roleId
) {
}
