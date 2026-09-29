package com.portal_cm.portal_cm.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentNotificationRepository
        extends JpaRepository<IncidentNotification, Integer>, JpaSpecificationExecutor<IncidentNotification> {
}
