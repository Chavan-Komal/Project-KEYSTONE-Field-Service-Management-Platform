package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.exception.InsufficientStockException;
import com.zidio.keystone.exception.InvalidTransitionException;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final PartUsageRepository partUsageRepository;
    private final TimeLogRepository timeLogRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final PartRepository partRepository;

    public WorkOrderService(
        WorkOrderRepository workOrderRepository,
        WorkOrderStatusHistoryRepository historyRepository,
        PartUsageRepository partUsageRepository,
        TimeLogRepository timeLogRepository,
        CustomerRepository customerRepository,
        SiteRepository siteRepository,
        UserRepository userRepository,
        PartRepository partRepository
    ) {
        this.workOrderRepository = workOrderRepository;
        this.historyRepository = historyRepository;
        this.partUsageRepository = partUsageRepository;
        this.timeLogRepository = timeLogRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.partRepository = partRepository;
    }

    @Value("${keystone.sla.hours.critical}") private long slaCriticalHours;
    @Value("${keystone.sla.hours.high}") private long slaHighHours;
    @Value("${keystone.sla.hours.medium}") private long slaMediumHours;
    @Value("${keystone.sla.hours.low}") private long slaLowHours;
    @Value("${keystone.sla.at-risk-threshold-percent}") private int atRiskThresholdPercent;

    // ---------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> listWorkOrders(WorkOrderStatus status, String q, Pageable pageable) {
        UserPrincipal principal = currentPrincipal();

        Specification<WorkOrder> spec = Specification.where(WorkOrderSpecifications.hasStatus(status))
            .and(WorkOrderSpecifications.matchesSearch(q));

        // Role scoping happens here, at the query level — not filtered out
        // after the fact in the controller. This is the actual boundary that
        // stops a technician or customer from ever seeing someone else's rows.
        spec = switch (principal.getRole()) {
            case TECHNICIAN -> spec.and(WorkOrderSpecifications.assignedToTechnician(principal.getId()));
            case CUSTOMER -> spec.and(WorkOrderSpecifications.forCustomer(principal.getCustomerId()));
            case DISPATCHER, MANAGER -> spec; // full visibility
        };

        Page<WorkOrder> page = workOrderRepository.findAll(spec, pageable);
        Page<WorkOrderResponse> mapped = page.map(wo -> WorkOrderResponse.summary(wo, computeSlaState(wo)));
        return PageResponse.from(mapped);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrder(UUID id) {
        WorkOrder wo = loadAndCheckAccess(id);

        List<StatusHistoryDto> history = historyRepository.findByWorkOrderIdOrderByChangedAtAsc(id)
            .stream().map(StatusHistoryDto::from).toList();
        List<PartUsageDto> parts = partUsageRepository.findByWorkOrderId(id)
            .stream().map(PartUsageDto::from).toList();
        List<TimeLogDto> timeLogs = timeLogRepository.findByWorkOrderId(id)
            .stream().map(TimeLogDto::from).toList();

        return WorkOrderResponse.detailed(wo, computeSlaState(wo), history, parts, timeLogs);
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    // F3 create + F9 customer self-service — both funnel through here so every
    // request enters the same governed pipeline (brief, Section 01/F9).
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    @Transactional
    public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request) {
        UserPrincipal principal = currentPrincipal();

        UUID customerId;
        if (principal.getRole() == Role.CUSTOMER) {
            customerId = principal.getCustomerId();
        } else {
            if (request.customerId() == null) {
                throw new IllegalArgumentException("customerId is required when raising a work order as staff.");
            }
            customerId = request.customerId();
        }

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        Site site = siteRepository.findById(request.siteId())
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + request.siteId()));

        // A site always belongs to a customer — never let a request attach a
        // work order to a site owned by a different customer (Section 05).
        if (!site.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("That site does not belong to this customer.");
        }

        User actor = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Instant now = Instant.now();
        WorkOrder wo = WorkOrder.builder()
            .code(generateCode())
            .title(request.title())
            .description(request.description())
            .priority(request.priority())
            .status(WorkOrderStatus.NEW)
            .customer(customer)
            .site(site)
            .slaDueAt(now.plus(Duration.ofHours(slaHoursFor(request.priority()))))
            .build();

        wo = workOrderRepository.save(wo);
        recordHistory(wo, null, WorkOrderStatus.NEW, actor, "Raised");

        return getWorkOrder(wo.getId());
    }

    // ---------------------------------------------------------------
    // Dispatch & assignment (F4)
    // ---------------------------------------------------------------

    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Transactional
    public WorkOrderResponse assign(UUID workOrderId, AssignRequest request) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + workOrderId));

        if (wo.getStatus().isTerminal()) {
            throw new InvalidTransitionException("Cannot assign a work order that is " + wo.getStatus());
        }

        User technician = userRepository.findById(request.technicianId())
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + request.technicianId()));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("Assignee must be a user with the TECHNICIAN role.");
        }

        User actor = userRepository.findById(currentPrincipal().getId()).orElseThrow();
        WorkOrderStatus previousStatus = wo.getStatus();

        wo.setAssignedTo(technician);
        if (previousStatus == WorkOrderStatus.NEW) {
            if (!previousStatus.canTransitionTo(WorkOrderStatus.ASSIGNED)) {
                throw new InvalidTransitionException("Cannot move from " + previousStatus + " to ASSIGNED");
            }
            wo.setStatus(WorkOrderStatus.ASSIGNED);
            wo = workOrderRepository.save(wo);
            recordHistory(wo, previousStatus, WorkOrderStatus.ASSIGNED, actor, "Assigned to " + technician.getName());
        } else {
            // Reassignment while already open — status doesn't change, but we
            // still keep an audit trail of who did the reassigning.
            wo = workOrderRepository.save(wo);
            recordHistory(wo, previousStatus, previousStatus, actor, "Reassigned to " + technician.getName());
        }

        return getWorkOrder(wo.getId());
    }

    // ---------------------------------------------------------------
    // Status transitions — the guarded lifecycle (Section 07)
    // ---------------------------------------------------------------

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public WorkOrderResponse transitionStatus(UUID workOrderId, StatusTransitionRequest request) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + workOrderId));

        WorkOrderStatus current = wo.getStatus();
        WorkOrderStatus target = request.toStatus();
        UserPrincipal principal = currentPrincipal();

        if (!current.canTransitionTo(target)) {
            throw new InvalidTransitionException(
                "Illegal transition: " + current + " -> " + target + " is not allowed.");
        }

        assertRoleCanPerformTransition(wo, current, target, principal);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        wo.setStatus(target);
        wo = workOrderRepository.save(wo);
        recordHistory(wo, current, target, actor, request.note());

        return getWorkOrder(wo.getId());
    }

    private void assertRoleCanPerformTransition(
        WorkOrder wo, WorkOrderStatus current, WorkOrderStatus target, UserPrincipal principal
    ) {
        switch (target) {
            case CANCELLED -> requireRole(principal, Role.DISPATCHER, Role.MANAGER);
            case CLOSED -> requireRole(principal, Role.MANAGER); // "only a manager can CLOSE" — Section 07.2
            case IN_PROGRESS -> {
                if (current == WorkOrderStatus.COMPLETED) {
                    // reopening — a dispatch decision, not fieldwork
                    requireRole(principal, Role.DISPATCHER, Role.MANAGER);
                } else {
                    requireAssignedTechnicianOrManager(wo, principal);
                }
            }
            case ON_HOLD, COMPLETED -> requireAssignedTechnicianOrManager(wo, principal);
            default -> { /* ASSIGNED is only reached via assign() */ }
        }
    }

    private void requireRole(UserPrincipal principal, Role... allowed) {
        if (Set.of(allowed).stream().noneMatch(r -> r == principal.getRole())) {
            throw new AccessDeniedException("Your role cannot perform this action.");
        }
    }

    private void requireAssignedTechnicianOrManager(WorkOrder wo, UserPrincipal principal) {
        boolean isManager = principal.getRole() == Role.MANAGER;
        boolean isAssignedTechnician = principal.getRole() == Role.TECHNICIAN
            && wo.getAssignedTo() != null
            && wo.getAssignedTo().getId().equals(principal.getId());

        if (!isManager && !isAssignedTechnician) {
            throw new AccessDeniedException(
                "Only the assigned technician (or a manager) can perform this action.");
        }
    }

    // ---------------------------------------------------------------
    // Parts usage (F6) — transactional stock decrement
    // ---------------------------------------------------------------

    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER')")
    @Transactional
    public WorkOrderResponse logPartUsage(UUID workOrderId, PartUsageRequest request) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + workOrderId));

        UserPrincipal principal = currentPrincipal();
        requireAssignedTechnicianOrManager(wo, principal);

        Part part = partRepository.findById(request.partId())
            .orElseThrow(() -> new ResourceNotFoundException("Part not found: " + request.partId()));

        // Stock can never go negative (F6 acceptance criteria) — checked and
        // decremented in the same transaction as the usage-log insert, so a
        // failure anywhere rolls back both together (Section 05 integrity rule).
        if (part.getStockQty() < request.qtyUsed()) {
            throw new InsufficientStockException(
                "Only " + part.getStockQty() + " units of " + part.getName() + " remain in stock.");
        }

        part.setStockQty(part.getStockQty() - request.qtyUsed());
        partRepository.save(part);

        PartUsage usage = PartUsage.builder()
            .workOrder(wo)
            .part(part)
            .qtyUsed(request.qtyUsed())
            .unitCostAtUse(part.getUnitCost())
            .build();
        partUsageRepository.save(usage);

        return getWorkOrder(workOrderId);
    }

    // ---------------------------------------------------------------
    // Time logging (F6)
    // ---------------------------------------------------------------

    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER')")
    @Transactional
    public WorkOrderResponse logTime(UUID workOrderId, TimeLogRequest request) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + workOrderId));

        UserPrincipal principal = currentPrincipal();
        requireAssignedTechnicianOrManager(wo, principal);

        User technician = userRepository.findById(principal.getId()).orElseThrow();

        TimeLog log = TimeLog.builder()
            .workOrder(wo)
            .technician(technician)
            .minutes(request.minutes())
            .note(request.note())
            .build();
        timeLogRepository.save(log);

        return getWorkOrder(workOrderId);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private WorkOrder loadAndCheckAccess(UUID id) {
        WorkOrder wo = workOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));

        UserPrincipal principal = currentPrincipal();
        switch (principal.getRole()) {
            case TECHNICIAN -> {
                if (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("This work order is not assigned to you.");
                }
            }
            case CUSTOMER -> {
                if (!wo.getCustomer().getId().equals(principal.getCustomerId())) {
                    throw new AccessDeniedException("This work order does not belong to your organisation.");
                }
            }
            case DISPATCHER, MANAGER -> { /* full access */ }
        }
        return wo;
    }

    private void recordHistory(WorkOrder wo, WorkOrderStatus from, WorkOrderStatus to, User actor, String note) {
        WorkOrderStatusHistory entry = WorkOrderStatusHistory.builder()
            .workOrder(wo)
            .fromStatus(from)
            .toStatus(to)
            .changedBy(actor)
            .note(note)
            .build();
        historyRepository.save(entry);
    }

    private String generateCode() {
        // Simple, readable sequential code (WO-1001, WO-1002, ...). Note: under
        // heavy concurrent writes this has a narrow race window between the
        // count and the save; acceptable at seed-scale, but a DB sequence would
        // be the production-grade fix.
        long next = 1000 + workOrderRepository.count() + 1;
        String candidate = "WO-" + next;
        while (workOrderRepository.findByCode(candidate).isPresent()) {
            next++;
            candidate = "WO-" + next;
        }
        return candidate;
    }

    private long slaHoursFor(Priority priority) {
        return switch (priority) {
            case CRITICAL -> slaCriticalHours;
            case HIGH -> slaHighHours;
            case MEDIUM -> slaMediumHours;
            case LOW -> slaLowHours;
        };
    }

    private SlaState computeSlaState(WorkOrder wo) {
        if (wo.getStatus().isTerminal()) {
            return SlaState.OK; // resolved jobs don't carry an active SLA risk
        }

        Instant now = Instant.now();
        if (now.isAfter(wo.getSlaDueAt())) {
            return SlaState.BREACHED;
        }

        Duration totalWindow = Duration.between(wo.getCreatedAt(), wo.getSlaDueAt());
        Duration remaining = Duration.between(now, wo.getSlaDueAt());
        if (totalWindow.isZero() || totalWindow.isNegative()) {
            return SlaState.AT_RISK;
        }

        double remainingPercent = (remaining.toMillis() * 100.0) / totalWindow.toMillis();
        return remainingPercent <= atRiskThresholdPercent ? SlaState.AT_RISK : SlaState.OK;
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
