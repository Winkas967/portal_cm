package com.portal_cm.portal_cm.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Anexo da ficha. O arquivo físico fica no servidor; aqui só o caminho relativo. */
@Entity
@Table(name = "incident_attachment")
@Getter
@Setter
@NoArgsConstructor
public class IncidentAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private IncidentNotification notification;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_path", nullable = false)
    private String storedPath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime uploadedAt;
}
