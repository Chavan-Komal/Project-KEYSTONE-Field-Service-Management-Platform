package com.zidio.keystone.controller;

import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.service.WorkOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
