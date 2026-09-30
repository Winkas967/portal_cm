package com.portal_cm.portal_cm.notification.sector;

import com.portal_cm.portal_cm.notification.sector.dto.SectorRequest;
import com.portal_cm.portal_cm.notification.sector.dto.SectorResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public List<SectorResponse> findAll(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return sectorService.findAll(includeInactive);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SectorResponse> create(@Valid @RequestBody SectorRequest request) {
        SectorResponse created = sectorService.create(request);
        return ResponseEntity.created(URI.create("/api/sectors/" + created.id())).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SectorResponse> update(@PathVariable Integer id, @Valid @RequestBody SectorRequest request) {
        return ResponseEntity.ok(sectorService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SectorResponse> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(sectorService.deactivate(id));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<SectorResponse> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(sectorService.reactivate(id));
    }
}
