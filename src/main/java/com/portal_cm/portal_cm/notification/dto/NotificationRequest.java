package com.portal_cm.portal_cm.notification.dto;

import com.portal_cm.portal_cm.notification.enums.MedicationErrorStage;
import com.portal_cm.portal_cm.notification.enums.PhlebitisType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record NotificationRequest(
        @NotNull(message = "Selecione o setor da ocorrência")
        Integer sectorId,

        @NotNull(message = "Informe a data e hora do evento")
        LocalDateTime eventDate,

        // seção 1
        @NotEmpty(message = "Marque pelo menos um tipo de incidente")
        Set<@NotNull Integer> IncidentTypeIds,

        MedicationErrorStage medicationErrorStage,

        PhlebitisType phlebitisType,

        @Size(max = 2000, message = "A descrição de 'Outros' deve ter no máximo 2000 caracteres")
        String otherIncidentDescriptionm,

        // seçao 2
        @Valid
        ProductDefectRequest productDefect,

        //seçao 3
        @Size(max = 5000, message = "A não conformidade deve ter no máximo 5000 caracteres")
        String nonconformity,

        //secao 4
        @NotBlank(message = "Preencha o nome do paciente")
        @Size(max = 150, message = "O nome do paciente deve ter no máximo 150 caracteres" )
        String patientName,

        @PastOrPresent(message = "A data de nascimento não pode ser futura")
        LocalDate patientBirthDate,

        @Size(max = 30, message = "A cor deve ter no máximo 30 caracteres")
        String patientColor,

        @Size(max = 2000, message = "O motivo deve ter no máximo 2000 caracteres")
        String visitReason,

        @Size(max = 30, message = "O prontuário deve ter no máximo 30 caracteres")
        String medicalRecordNumber,

        @Size(max = 30, message = "O atendimento deve ter no máximo 30 caracteres")
        String attendanceNumber,

        @Size(max = 50, message = "A enfermaria/leito deve ter no máximo 50 caracteres")
        String wardBed,

        @NotBlank(message = "Descreva o evento")
        @Size(max = 10000, message = "A descrição deve ter no máximo 10000 caracteres")
        String eventDescription,

        @Size(max = 5000, message = "As ações imediatas devem ter no máximo 5000 caracteres")
        String immediateActions


) {
}
