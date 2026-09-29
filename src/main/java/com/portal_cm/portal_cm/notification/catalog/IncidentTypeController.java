package com.portal_cm.portal_cm.notification.catalog;


import com.portal_cm.portal_cm.notification.catalog.dto.IncidentTypeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incident-types")
public class IncidentTypeController {

    private final IncidentTypeRepository incidentTypeRepository;

    public IncidentTypeController(IncidentTypeRepository incidentTypeRepository) {
        this.incidentTypeRepository = incidentTypeRepository;
    }

    @GetMapping
    public List<IncidentTypeResponse> findAll() {
        return incidentTypeRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(t -> new IncidentTypeResponse(t.getId(), t.getName(), t.getCode())).toList();
    }
}
