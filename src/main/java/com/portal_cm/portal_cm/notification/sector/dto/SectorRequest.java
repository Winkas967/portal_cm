package com.portal_cm.portal_cm.notification.sector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectorRequest(
        @NotBlank(message = "Preencha o nome do setor")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name
) {
}
