package com.portal_cm.portal_cm.notification;

import com.portal_cm.portal_cm.exception.NotFoundException;
import com.portal_cm.portal_cm.notification.catalog.IncidentType;
import com.portal_cm.portal_cm.notification.catalog.IncidentTypeRepository;
import com.portal_cm.portal_cm.notification.catalog.dto.IncidentTypeResponse;
import com.portal_cm.portal_cm.notification.dto.*;
import com.portal_cm.portal_cm.notification.enums.DefectKind;
import com.portal_cm.portal_cm.notification.sector.Sector;
import com.portal_cm.portal_cm.notification.sector.SectorRepository;
import com.portal_cm.portal_cm.notification.storage.FileStorageService;
import com.portal_cm.portal_cm.notification.storage.StoredFile;
import com.portal_cm.portal_cm.security.UserPrincipal;
import com.portal_cm.portal_cm.users.user.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class NotificationService {

    public static final int MAX_FILES = 5;
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final IncidentNotificationRepository notificationRepository;
    private final IncidentAttachmentRepository attachmentRepository;
    private final SectorRepository sectorRepository;
    private final IncidentTypeRepository incidentTypeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ZoneId zone;

    public NotificationService(
            IncidentNotificationRepository notificationRepository,
            IncidentAttachmentRepository attachmentRepository,
            SectorRepository sectorRepository,
            IncidentTypeRepository incidentTypeRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            @Value("${app.timezone}") String timezone
    ) {
        this.notificationRepository = notificationRepository;
        this.attachmentRepository = attachmentRepository;
        this.sectorRepository = sectorRepository;
        this.incidentTypeRepository = incidentTypeRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.zone = ZoneId.of(timezone);
    }

    // ------------------------------------------------------------------ criar

    @Transactional
    public NotificationDetailResponse create(NotificationRequest request, List<MultipartFile> files, Integer currentUserId) {
        List<MultipartFile> uploads = files == null ? List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();

        if (uploads.size() > MAX_FILES) {
            throw new IllegalArgumentException("Envie no máximo " + MAX_FILES + " arquivos por ficha.");
        }
        uploads.forEach(fileStorageService::validate);

        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() -> new IllegalArgumentException("Não foi encontrado setor com o ID informado."));
        if (!sector.isActive()) {
            throw new IllegalArgumentException("O setor selecionado está inativo.");
        }

        List<IncidentType> types = incidentTypeRepository.findAllById(request.incidentTypeIds());
        if (types.size() != request.incidentTypeIds().size()) {
            throw new IllegalArgumentException("Um ou mais tipos de incidente não foram encontrados.");
        }
        if (types.stream().anyMatch(t -> !t.isActive())) {
            throw new IllegalArgumentException("Um ou mais tipos de incidente estão inativos.");
        }
        Set<String> codes = new HashSet<>();
        types.stream().map(IncidentType::getCode).filter(Objects::nonNull).forEach(codes::add);

        OffsetDateTime now = OffsetDateTime.now(zone);
        OffsetDateTime eventDate = request.eventDate().atZone(zone).toOffsetDateTime();
        if (eventDate.isAfter(now)) {
            throw new IllegalArgumentException("A data do evento não pode ser futura.");
        }
        if (request.patientBirthDate() != null && request.patientBirthDate().isAfter(eventDate.toLocalDate())) {
            throw new IllegalArgumentException("A data de nascimento não pode ser posterior à data do evento.");
        }

        validateConditional(codes.contains(IncidentType.MEDICATION_ERROR), request.medicationErrorStage() != null,
                "Informe a fase do erro de medicação (PRESCRICAO, DISPENSACAO ou ADMINISTRACAO).",
                "A fase do erro de medicação só pode ser informada quando 'Falha / erro de medicação' estiver marcado.");
        validateConditional(codes.contains(IncidentType.PHLEBITIS), request.phlebitisType() != null,
                "Informe o tipo de flebite (QUIMICA, MECANICA ou INFECCIOSA).",
                "O tipo de flebite só pode ser informado quando 'Flebite' estiver marcado.");
        validateConditional(codes.contains(IncidentType.OTHER), hasText(request.otherIncidentDescription()),
                "Descreva o incidente marcado como 'Outros'.",
                "A descrição de 'Outros' só pode ser informada quando 'Outros' estiver marcado.");

        IncidentNotification notification = new IncidentNotification();
        notification.setSector(sector);
        notification.setNotifier(userRepository.getReferenceById(currentUserId));
        notification.setNotificationDate(now);
        notification.setCreatedAt(now);
        notification.setEventDate(eventDate);
        notification.getIncidentTypes().addAll(types);
        notification.setMedicationErrorStage(request.medicationErrorStage());
        notification.setPhlebitisType(request.phlebitisType());
        notification.setOtherIncidentDescription(clean(request.otherIncidentDescription()));
        notification.setNonconformity(clean(request.nonconformity()));
        notification.setPatientName(request.patientName().trim());
        notification.setPatientBirthDate(request.patientBirthDate());
        notification.setPatientColor(clean(request.patientColor()));
        notification.setVisitReason(clean(request.visitReason()));
        notification.setMedicalRecordNumber(clean(request.medicalRecordNumber()));
        notification.setAttendanceNumber(clean(request.attendanceNumber()));
        notification.setWardBed(clean(request.wardBed()));
        notification.setEventDescription(request.eventDescription().trim());
        notification.setImmediateActions(clean(request.immediateActions()));

        IncidentProductDefect defect = buildProductDefect(codes, request.productDefect());
        if (defect != null) {
            notification.attachProductDefect(defect);
        }

        // Grava os arquivos no disco. Se a transação não for confirmada,
        // os arquivos já gravados são apagados (não sobra arquivo sem ficha).
        List<StoredFile> storedFiles = new ArrayList<>();
        deleteFilesIfTransactionFails(storedFiles);
        for (MultipartFile upload : uploads) {
            StoredFile stored = fileStorageService.store(upload);
            storedFiles.add(stored);

            IncidentAttachment attachment = new IncidentAttachment();
            attachment.setOriginalName(stored.originalName());
            attachment.setStoredPath(stored.relativePath());
            attachment.setContentType(stored.contentType());
            attachment.setSizeBytes(stored.size());
            attachment.setUploadedAt(now);
            notification.addAttachment(attachment);
        }

        IncidentNotification saved = notificationRepository.saveAndFlush(notification);
        return toDetail(saved);
    }

    // --------------------------------------------------------------- consultar

    @Transactional(readOnly = true)
    public PageResponse<NotificationSummaryResponse> search(Integer sectorId, Integer incidentTypeId,
                                                            LocalDate from, LocalDate to,
                                                            Pageable pageable, UserPrincipal currentUser) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }

        boolean admin = isAdmin(currentUser);
        Integer userId = currentUser.getId();

        Specification<IncidentNotification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!admin) {
                predicates.add(cb.equal(root.get("notifier").get("id"), userId));
            }
            if (sectorId != null) {
                predicates.add(cb.equal(root.get("sector").get("id"), sectorId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("eventDate"),
                        from.atStartOfDay(zone).toOffsetDateTime()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<OffsetDateTime>get("eventDate"),
                        to.plusDays(1).atStartOfDay(zone).toOffsetDateTime()));
            }
            if (incidentTypeId != null) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<IncidentNotification> subRoot = sub.from(IncidentNotification.class);
                Join<IncidentNotification, IncidentType> type = subRoot.join("incidentTypes");
                sub.select(subRoot.<Integer>get("id"))
                        .where(cb.equal(subRoot.get("id"), root.get("id")),
                                cb.equal(type.get("id"), incidentTypeId));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(notificationRepository.findAll(spec, pageable).map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse findById(Integer id, UserPrincipal currentUser) {
        return toDetail(findVisible(id, currentUser));
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(Integer notificationId, Integer attachmentId, UserPrincipal currentUser) {
        findVisible(notificationId, currentUser);
        IncidentAttachment attachment = attachmentRepository.findByIdAndNotification_Id(attachmentId, notificationId)
                .orElseThrow(() -> new NotFoundException("Anexo não encontrado."));

        Resource resource = fileStorageService.load(attachment.getStoredPath());
        return new AttachmentDownload(resource, attachment.getOriginalName(), attachment.getContentType());
    }

    public record AttachmentDownload(Resource resource, String originalName, String contentType) {
    }

    // ----------------------------------------------------------------- regras

    /** Quem não é ADMIN só enxerga as fichas que registrou. Para os outros, a ficha "não existe" (404). */
    private IncidentNotification findVisible(Integer id, UserPrincipal currentUser) {
        IncidentNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ficha não encontrada."));

        if (!isAdmin(currentUser) && !notification.getNotifier().getId().equals(currentUser.getId())) {
            throw new NotFoundException("Ficha não encontrada.");
        }
        return notification;
    }

    private boolean isAdmin(UserPrincipal user) {
        return user.getAuthorities().stream().anyMatch(a -> ADMIN_AUTHORITY.equals(a.getAuthority()));
    }

    /** Campo obrigatório quando o tipo está marcado, e proibido quando não está. */
    private void validateConditional(boolean typeMarked, boolean valueInformed, String requiredMessage, String forbiddenMessage) {
        if (typeMarked && !valueInformed) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (!typeMarked && valueInformed) {
            throw new IllegalArgumentException(forbiddenMessage);
        }
    }

    private IncidentProductDefect buildProductDefect(Set<String> codes, ProductDefectRequest data) {
        boolean pharmaco = codes.contains(IncidentType.PHARMACOVIGILANCE);
        boolean techno = codes.contains(IncidentType.TECHNOVIGILANCE);

        if (pharmaco && techno) {
            throw new IllegalArgumentException("Farmacovigilância e tecnovigilância devem ser notificadas em fichas separadas.");
        }
        if (!pharmaco && !techno) {
            if (data != null) {
                throw new IllegalArgumentException("Os dados de medicamento/equipamento só podem ser informados quando "
                        + "farmacovigilância ou tecnovigilância estiver marcada.");
            }
            return null;
        }
        if (data == null) {
            throw new IllegalArgumentException(pharmaco
                    ? "Preencha os dados do medicamento (seção 2)."
                    : "Preencha os dados do equipamento (seção 2).");
        }

        IncidentProductDefect defect = new IncidentProductDefect();
        if (pharmaco) {
            require(data.productName(), "Informe o nome do medicamento.");
            require(data.manufacturer(), "Informe o fabricante do medicamento.");
            require(data.batch(), "Informe o lote do medicamento.");
            if (data.expiryDate() == null) {
                throw new IllegalArgumentException("Informe a validade do medicamento.");
            }
            defect.setKind(DefectKind.MEDICAMENTO);
            defect.setProductName(data.productName().trim());
            defect.setManufacturer(data.manufacturer().trim());
            defect.setBatch(data.batch().trim());
            defect.setExpiryDate(data.expiryDate());
        } else {
            require(data.equipment(), "Informe o equipamento.");
            defect.setKind(DefectKind.EQUIPAMENTO);
            defect.setEquipment(data.equipment().trim());
            defect.setSerialNumber(clean(data.serialNumber()));
            defect.setLastPreventiveMaintenance(data.lastPreventiveMaintenance());
            defect.setAssetNumber(clean(data.assetNumber()));
        }
        return defect;
    }

    private void deleteFilesIfTransactionFails(List<StoredFile> storedFiles) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storedFiles.forEach(f -> fileStorageService.deleteQuietly(f.relativePath()));
                }
            }
        });
    }

    private static void require(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Tira espaços das pontas; texto vazio vira null. */
    private static String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    // ------------------------------------------------------------- respostas

    private NotificationSummaryResponse toSummary(IncidentNotification n) {
        List<String> typeNames = n.getIncidentTypes().stream()
                .map(IncidentType::getName)
                .sorted()
                .toList();

        return new NotificationSummaryResponse(
                n.getId(),
                n.getNotificationDate().atZoneSameInstant(zone).toOffsetDateTime(),
                n.getEventDate().atZoneSameInstant(zone).toOffsetDateTime(),
                new NamedRef(n.getSector().getId(), n.getSector().getName()),
                new NamedRef(n.getNotifier().getId(), n.getNotifier().getName()),
                n.getPatientName(),
                typeNames,
                n.getAttachments().size());
    }

    private NotificationDetailResponse toDetail(IncidentNotification n) {
        List<IncidentTypeResponse> types = n.getIncidentTypes().stream()
                .sorted(Comparator.comparing(IncidentType::getName))
                .map(t -> new IncidentTypeResponse(t.getId(), t.getName(), t.getCode()))
                .toList();

        IncidentProductDefect d = n.getProductDefect();
        ProductDefectResponse defect = d == null ? null : new ProductDefectResponse(
                d.getKind(), d.getProductName(), d.getManufacturer(), d.getBatch(), d.getExpiryDate(),
                d.getEquipment(), d.getSerialNumber(), d.getLastPreventiveMaintenance(), d.getAssetNumber());

        List<AttachmentResponse> attachments = n.getAttachments().stream()
                .map(a -> new AttachmentResponse(
                        a.getId(),
                        a.getOriginalName(),
                        a.getContentType(),
                        a.getSizeBytes(),
                        a.getUploadedAt().atZoneSameInstant(zone).toOffsetDateTime(),
                        "/api/notifications/" + n.getId() + "/attachments/" + a.getId()))
                .toList();

        return new NotificationDetailResponse(
                n.getId(),
                n.getNotificationDate().atZoneSameInstant(zone).toOffsetDateTime(),
                n.getEventDate().atZoneSameInstant(zone).toOffsetDateTime(),
                new NamedRef(n.getSector().getId(), n.getSector().getName()),
                new NamedRef(n.getNotifier().getId(), n.getNotifier().getName()),
                types,
                n.getMedicationErrorStage(),
                n.getPhlebitisType(),
                n.getOtherIncidentDescription(),
                defect,
                n.getNonconformity(),
                n.getPatientName(),
                n.getPatientBirthDate(),
                n.getPatientColor(),
                n.getVisitReason(),
                n.getMedicalRecordNumber(),
                n.getAttendanceNumber(),
                n.getWardBed(),
                n.getEventDescription(),
                n.getImmediateActions(),
                attachments);
    }
}
