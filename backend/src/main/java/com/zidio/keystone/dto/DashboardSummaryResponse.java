package com.zidio.keystone.dto;

import com.zidio.keystone.domain.WorkOrderStatus;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
    Map<WorkOrderStatus, Long> countsByStatus,
    long overdueCount,
    double slaComplianceRate,
    List<TechnicianLoad> byTechnician
) {
    public record TechnicianLoad(String technicianName, long openCount) {}
}
