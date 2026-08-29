package com.zidio.keystone.service;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F7 — "A scheduled job flags work orders at risk of, or in, breach."
 *
 * Runs every 5 minutes and logs breaches. Swap the TODO for a real
 * notification (email/in-app) when you're ready to wire one up.
 *
 * Logs each breach once, not on every run — {@code alreadyWarned} tracks
 * which open work orders have already fired a warning so the console
 * doesn't repeat the same line every 5 minutes for the life of the process.
 * An order drops out of the set once it's no longer open or no longer
 * breached, so a reopened order that breaches again gets a fresh warning.
 */
@Component
public class SlaMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitorScheduler.class);

    private final WorkOrderRepository workOrderRepository;
    private final Set<UUID> alreadyWarned = ConcurrentHashMap.newKeySet();

    public SlaMonitorScheduler(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @Scheduled(fixedRateString = "${keystone.sla.check-interval-ms:300000}")
    public void checkSlaBreaches() {
        Instant now = Instant.now();
        List<WorkOrder> openOrders = workOrderRepository.findByStatusNotIn(
            List.of(WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED)
        );

        Set<UUID> stillBreached = ConcurrentHashMap.newKeySet();
        for (WorkOrder wo : openOrders) {
            if (now.isAfter(wo.getSlaDueAt())) {
                stillBreached.add(wo.getId());
                if (alreadyWarned.add(wo.getId())) {
                    log.warn("SLA BREACH: {} ({}) was due at {}", wo.getCode(), wo.getTitle(), wo.getSlaDueAt());
                    // TODO: push to a notifications table / send an email to the manager.
                }
            }
        }
        alreadyWarned.retainAll(stillBreached);
    }
}
