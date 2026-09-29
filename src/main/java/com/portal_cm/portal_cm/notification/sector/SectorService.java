package com.portal_cm.portal_cm.notification.sector;

import com.portal_cm.portal_cm.notification.sector.dto.SectorRequest;
import com.portal_cm.portal_cm.notification.sector.dto.SectorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;

    public SectorService(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> findAll(boolean includeInactive) {
        List<Sector> sectors = includeInactive
                ? sectorRepository.findAllByOrderByNameAsc()
                : sectorRepository.findAllByIsActiveTrueOrderByNameAsc();
        return sectors.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SectorResponse create(SectorRequest request) {
        String name = request.name().trim();
        if (sectorRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Já existe um setor com este nome.");
        }

        Sector sector = new Sector();
        sector.setName(name);
        return toResponse(sectorRepository.save(sector));
    }

    @Transactional
    public SectorResponse update(Integer id, SectorRequest request) {
        Sector sector = findSector(id);
        String name = request.name().trim();

        if (!name.equalsIgnoreCase(sector.getName()) && sectorRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Já existe um setor com este nome.");
        }

        sector.setName(name);
        return toResponse(sectorRepository.save(sector));
    }

    @Transactional
    public SectorResponse deactivate(Integer id) {
        Sector sector = findSector(id);
        if (!sector.isActive()) {
            throw new IllegalArgumentException(("O setor já está inativo."));
        }
        sector.setActive(false);
        return toResponse(sectorRepository.save(sector));
    }

    @Transactional
    public SectorResponse reactivate(Integer id) {
        Sector sector = findSector(id);
        if (sector.isActive()) {
            throw new IllegalArgumentException("O setor já está ativo.");
        }
        sector.setActive(true);
        return toResponse(sectorRepository.save(sector));
    }

    private Sector findSector(Integer id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Não foi encontrado setor com o ID informado"));
    }

    private SectorResponse toResponse(Sector sector) {
        return new SectorResponse(sector.getId(), sector.getName(), sector.isActive());
    }
}
