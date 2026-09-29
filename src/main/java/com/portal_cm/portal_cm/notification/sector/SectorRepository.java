package com.portal_cm.portal_cm.notification.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorRepository extends JpaRepository<Sector, Integer> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    List<Sector> findAllByOrderByNameAsc();
    List<Sector> findAllByIsActiveTrueOrderByNameAsc();
}
