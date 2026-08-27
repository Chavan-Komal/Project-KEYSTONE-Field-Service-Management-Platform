package com.zidio.keystone.dto;

import com.zidio.keystone.domain.TimeLog;

import java.util.UUID;

public record TimeLogDto(UUID id, UUID technicianId, String technicianName, int minutes, String note) {
    public static TimeLogDto from(TimeLog t) {
        return new TimeLogDto(t.getId(), t.getTechnician().getId(), t.getTechnician().getName(), t.getMinutes(), t.getNote());
    }
}
