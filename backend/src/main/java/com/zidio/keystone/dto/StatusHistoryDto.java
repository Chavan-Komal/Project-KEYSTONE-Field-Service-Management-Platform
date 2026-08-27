package com.zidio.keystone.dto;

import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.domain.WorkOrderStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryDto(
    UUID id,
    WorkOrderStatus fromStatus,
    WorkOrderStatus toStatus,
    String changedBy,
    Instant changedAt,
    String note
) {
    public static StatusHistoryDto from(WorkOrderStatusHistory h) {
        return new StatusHistoryDto(
            h.getId(),
            h.getFromStatus(),
            h.getToStatus(),
            h.getChangedBy().getName(),
            h.getChangedAt(),
            h.getNote()
        );
    }
}
