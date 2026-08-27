package com.zidio.keystone.service;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.DashboardSummaryResponse;
import com.zidio.keystone.repository.WorkOrderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * F7 (SLA visibility) + F8 (dashboard & reporting). Manager-only, per Section 03.
 */
@Service
public class ReportService {

    private final WorkOrderRepository workOrderRepository;

    public ReportService(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Map<WorkOrderStatus, Long> countsByStatus = new EnumMap<>(WorkOrderStatus.class);
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            countsByStatus.put(status, workOrderRepository.countByStatus(status));
        }

        List<WorkOrder> openOrders = workOrderRepository.findByStatusNotIn(
            List.of(WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED)
        );

        Instant now = Instant.now();
        long overdueCount = openOrders.stream()
            .filter(wo -> now.isAfter(wo.getSlaDueAt()))
            .count();

        // SLA compliance: of everything closed so far, what fraction closed
        // before its SLA due date. Uses updatedAt as a proxy for "closed at" —
        // a dedicated closedAt timestamp column would be the more precise fix.
        long closedCount = countsByStatus.getOrDefault(WorkOrderStatus.CLOSED, 0L);
        double complianceRate;
        if (closedCount == 0) {
            complianceRate = 1.0;
        } else {
            List<WorkOrder> closedOrders = workOrderRepository.findAll().stream()
                .filter(wo -> wo.getStatus() == WorkOrderStatus.CLOSED)
                .toList();
            long metSla = closedOrders.stream()
                .filter(wo -> !wo.getUpdatedAt().isAfter(wo.getSlaDueAt()))
                .count();
            complianceRate = (double) metSla / closedOrders.size();
        }

        Map<String, Long> byTechRaw = openOrders.stream()
            .filter(wo -> wo.getAssignedTo() != null)
            .collect(Collectors.groupingBy(wo -> wo.getAssignedTo().getName(), Collectors.counting()));

        List<DashboardSummaryResponse.TechnicianLoad> byTechnician = byTechRaw.entrySet().stream()
            .map(e -> new DashboardSummaryResponse.TechnicianLoad(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingLong(DashboardSummaryResponse.TechnicianLoad::openCount).reversed())
            .toList();

        return new DashboardSummaryResponse(countsByStatus, overdueCount, complianceRate, byTechnician);
    }
}
