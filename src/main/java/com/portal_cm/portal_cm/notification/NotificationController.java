package com.portal_cm.portal_cm.notification;


import com.portal_cm.portal_cm.notification.dto.NotificationDetailResponse;
import com.portal_cm.portal_cm.notification.dto.NotificationRequest;
import com.portal_cm.portal_cm.notification.dto.NotificationSummaryResponse;
import com.portal_cm.portal_cm.notification.dto.PageResponse;
import com.portal_cm.portal_cm.security.UserPrincipal;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NotificationDetailResponse> create(
            @Valid @RequestPart("data")NotificationRequest data,
            @RequestPart(value = "files", required = false)List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        NotificationDetailResponse created = notificationService.create(data, files, currentUser.getId());
        return ResponseEntity.created(URI.create(("/api/notifications/" + created.id()))).body(created);
    }

    @GetMapping
    public PageResponse<NotificationSummaryResponse> search(
            @RequestParam(required = false) Integer sectorId,
            @RequestParam(required = false) Integer incidentTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "notificationDate", direction = Sort.Direction.DESC)Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return notificationService.search(sectorId, incidentTypeId, from, to, pageable, currentUser);
    }

    @GetMapping("/{id}")
    public NotificationDetailResponse findById(@PathVariable Integer id,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        return notificationService.findById(id, currentUser);
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Integer id,
                                                       @PathVariable Integer attachmentId,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        NotificationService.AttachmentDownload file = notificationService.downloadAttachment(id, attachmentId, currentUser);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
