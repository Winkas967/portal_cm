package com.portal_cm.portal_cm.notification.catalog;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "incident_type")
@Getter
@NoArgsConstructor
public class IncidentType {

    public static final String MEDICATION_ERROR = "MEDICATION_ERROR";
    public static final String PHLEBITIS = "PHLEBITIS";
    public static final String OTHER = "OTHER";
    public static final String TECHNOVIGILANCE = "TECHNOVIGILANCE";
    public static final String PHARMACOVIGILANCE = "PHARMACOVIGILANCE";

    @Id
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "active", nullable = false)
    private boolean active;
}
