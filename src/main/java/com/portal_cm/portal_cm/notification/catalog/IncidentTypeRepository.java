package com.portal_cm.portal_cm.notification.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentTypeRepository extends JpaRepository<IncidentType, Integer> {
    List<IncidentType> findAllByActiveTrueOrderByNameAsc();
}
