package com.portal_cm.portal_cm.notification;

import com.portal_cm.portal_cm.notification.enums.DefectKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "incident_product_defect")
@Getter
@Setter
@NoArgsConstructor
public class IncidentProductDefect {

    @Id
    @Column(name = "notification_id")
    private Integer notificationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id")
    private IncidentNotification notification;

    @Convert(converter = DefectKind.DbConverter.class)
    @Column(name = "kind", nullable = false)
    private DefectKind kind;

    // Medicamento
    @Column(name = "product_name")
    private String productName;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "batch")
    private String batch;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Equipamento
    @Column(name = "equipment")
    private String equipment;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "last_preventive_maintenance")
    private LocalDate lastPreventiveMaintenance;

    @Column(name = "asset_number")
    private String assetNumber;
}
