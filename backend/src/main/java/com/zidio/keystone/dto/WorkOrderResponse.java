package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Priority;
import com.zidio.keystone.domain.SlaState;
import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The API's public shape for a work order — entities are never serialised
 * directly to the client (Section 06.2). `history`, `parts`, and `timeLogs`
 * are populated for the single-item GET and left null for list views to keep
 * board/list payloads light.
 */
public record WorkOrderResponse(
    UUID id,
    String code,
    String title,
    String description,
    Priority priority,
    WorkOrderStatus status,
    UUID customerId,
    String customerName,
    UUID siteId,
    String siteName,
    UUID assignedTo,
    String assignedToName,
    Instant slaDueAt,
    SlaState slaState,
    Instant createdAt,
    List<StatusHistoryDto> history,
    List<PartUsageDto> parts,
    List<TimeLogDto> timeLogs
) {

    public static WorkOrderResponse summary(WorkOrder wo, SlaState slaState) {
        return new WorkOrderResponse(
            wo.getId(),
            wo.getCode(),
            wo.getTitle(),
            wo.getDescription(),
            wo.getPriority(),
            wo.getStatus(),
            wo.getCustomer().getId(),
            wo.getCustomer().getName(),
            wo.getSite().getId(),
            wo.getSite().getName(),
            wo.getAssignedTo() != null ? wo.getAssignedTo().getId() : null,
            wo.getAssignedTo() != null ? wo.getAssignedTo().getName() : null,
            wo.getSlaDueAt(),
            slaState,
            wo.getCreatedAt(),
            null,
            null,
            null
        );
    }

    public static WorkOrderResponse detailed(
        WorkOrder wo,
        SlaState slaState,
        List<StatusHistoryDto> history,
        List<PartUsageDto> parts,
        List<TimeLogDto> timeLogs
    ) {
        return new WorkOrderResponse(
            wo.getId(),
            wo.getCode(),
            wo.getTitle(),
            wo.getDescription(),
            wo.getPriority(),
            wo.getStatus(),
            wo.getCustomer().getId(),
            wo.getCustomer().getName(),
            wo.getSite().getId(),
            wo.getSite().getName(),
            wo.getAssignedTo() != null ? wo.getAssignedTo().getId() : null,
            wo.getAssignedTo() != null ? wo.getAssignedTo().getName() : null,
            wo.getSlaDueAt(),
            slaState,
            wo.getCreatedAt(),
            history,
            parts,
            timeLogs
        );
    }
}
