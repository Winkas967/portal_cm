package com.portal_cm.portal_cm.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentAttachmentRepository extends JpaRepository<IncidentAttachment, Integer> {
    Optional<IncidentAttachment> findByIdAndNotification_Id(Integer id, Integer notificationId);
}
