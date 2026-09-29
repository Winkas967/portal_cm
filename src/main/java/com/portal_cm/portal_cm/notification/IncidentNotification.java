package com.portal_cm.portal_cm.notification;

import com.portal_cm.portal_cm.notification.catalog.IncidentType;
import com.portal_cm.portal_cm.notification.enums.MedicationErrorStage;
import com.portal_cm.portal_cm.notification.enums.PhlebitisType;
import com.portal_cm.portal_cm.notification.sector.Sector;
import com.portal_cm.portal_cm.users.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ficha FOR-NSP-001. As colunas da parte "Reservado ao NSP" existem no banco,
 * mas não são mapeadas aqui porque ninguém avalia a ficha por enquanto.
 */
@Entity
@Table(name = "incident_notification")
@Getter
@Setter
@NoArgsConstructor
public class IncidentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notifier_id", nullable = false)
    private User notifier;

    @Column(name = "notification_date", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime notificationDate;

    @Column(name = "event_date", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime eventDate;

    // Seção 1
    @ManyToMany
    @JoinTable(
            name = "incident_notification_type",
            joinColumns = @JoinColumn(name = "notification_id"),
            inverseJoinColumns = @JoinColumn(name = "incident_type_id"))
    private Set<IncidentType> incidentTypes = new HashSet<>();

    @Convert(converter = MedicationErrorStage.DbConverter.class)
    @Column(name = "medication_error_stage")
    private MedicationErrorStage medicationErrorStage;

    @Convert(converter = PhlebitisType.DbConverter.class)
    @Column(name = "phlebitis_type")
    private PhlebitisType phlebitisType;

    @Column(name = "other_incident_description", columnDefinition = "text")
    private String otherIncidentDescription;

    // Seção 2
    @OneToOne(mappedBy = "notification", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IncidentProductDefect productDefect;

    // Seção 3
    @Column(name = "nonconformity", columnDefinition = "text")
    private String nonconformity;

    // Seção 4: paciente
    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_birth_date")
    private LocalDate patientBirthDate;

    @Column(name = "patient_color")
    private String patientColor;

    @Column(name = "visit_reason", columnDefinition = "text")
    private String visitReason;

    @Column(name = "medical_record_number")
    private String medicalRecordNumber;

    @Column(name = "attendance_number")
    private String attendanceNumber;

    @Column(name = "ward_bed")
    private String wardBed;

    @Column(name = "event_description", nullable = false, columnDefinition = "text")
    private String eventDescription;

    @Column(name = "immediate_actions", columnDefinition = "text")
    private String immediateActions;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<IncidentAttachment> attachments = new ArrayList<>();

    public void attachProductDefect(IncidentProductDefect defect) {
        defect.setNotification(this);
        this.productDefect = defect;
    }

    public void addAttachment(IncidentAttachment attachment) {
        attachment.setNotification(this);
        this.attachments.add(attachment);
    }
}