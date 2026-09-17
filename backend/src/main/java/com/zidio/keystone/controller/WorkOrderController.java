package com.zidio.keystone.controller;

import com.zidio.keystone.domain.WorkOrderAttachment;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.service.WorkOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-orders")
@Tag(name = "Work Orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public PageResponse<WorkOrderResponse> list(
        @RequestParam(required = false) WorkOrderStatus status,
        @RequestParam(required = false) String q,
        Pageable pageable
    ) {
        return workOrderService.listWorkOrders(status, q, pageable);
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable UUID id) {
        return workOrderService.getWorkOrder(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse create(@Valid @RequestBody CreateWorkOrderRequest request) {
        return workOrderService.createWorkOrder(request);
    }

    @PostMapping("/{id}/assign")
    public WorkOrderResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest request) {
        return workOrderService.assign(id, request);
    }

    @GetMapping("/{id}/nearest-technicians")
    public java.util.List<TechnicianDto> nearestTechnicians(@PathVariable UUID id) {
        return workOrderService.nearestTechnicians(id);
    }

    @PostMapping("/{id}/status")
    public WorkOrderResponse transitionStatus(@PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return workOrderService.transitionStatus(id, request);
    }

    @PostMapping("/{id}/parts")
    public WorkOrderResponse logParts(@PathVariable UUID id, @Valid @RequestBody PartUsageRequest request) {
        return workOrderService.logPartUsage(id, request);
    }

    @PostMapping("/{id}/time")
    public WorkOrderResponse logTime(@PathVariable UUID id, @Valid @RequestBody TimeLogRequest request) {
        return workOrderService.logTime(id, request);
    }

    // ---------------------------------------------------------------
    // Attachments (customer photos of the issue)
    // ---------------------------------------------------------------

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse addAttachment(
        @PathVariable UUID id,
        @RequestParam("file") MultipartFile file
    ) {
        return workOrderService.addAttachment(id, file);
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(
        @PathVariable UUID id,
        @PathVariable UUID attachmentId
    ) {
        WorkOrderAttachment attachment = workOrderService.getAttachment(id, attachmentId);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(attachment.getFilename()).build().toString())
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
            .body(attachment.getData());
    }
}
