package com.zidio.keystone.service;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.Site;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.MapPinDto;
import com.zidio.keystone.repository.SiteRepository;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.repository.WorkOrderRepository;
import com.zidio.keystone.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Backs the tracking map (Manager: org-wide; Customer: their own sites and
 * whoever's assigned to their open requests). Read-only, built from the same
 * geocoded site/technician-base coordinates the nearest-technician feature
 * uses — no live GPS involved.
 */
@Service
public class MapService {

    private static final List<WorkOrderStatus> TERMINAL = List.of(WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED);

    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;

    public MapService(SiteRepository siteRepository, UserRepository userRepository, WorkOrderRepository workOrderRepository) {
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Transactional(readOnly = true)
    public List<MapPinDto> managerOverview() {
        List<MapPinDto> pins = new ArrayList<>();

        for (User technician : userRepository.findByRoleOrderByNameAsc(Role.TECHNICIAN)) {
            if (technician.getBaseLatitude() != null && technician.getBaseLongitude() != null) {
                pins.add(MapPinDto.technician(technician.getId(), technician.getName(), technician.getBaseLatitude(), technician.getBaseLongitude()));
            }
        }

        for (WorkOrder wo : workOrderRepository.findByStatusNotInWithAssignee(TERMINAL)) {
            Site site = wo.getSite();
            if (site.getLatitude() != null && site.getLongitude() != null) {
                String assignedToName = wo.getAssignedTo() != null ? wo.getAssignedTo().getName() : null;
                pins.add(MapPinDto.workOrder(
                    wo.getId(), wo.getCode() + " — " + wo.getTitle(),
                    site.getLatitude(), site.getLongitude(),
                    wo.getStatus().name(), wo.getPriority().name(), assignedToName
                ));
            }
        }

        return pins;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Transactional(readOnly = true)
    public List<MapPinDto> customerOverview() {
        var principal = currentPrincipal();
        List<MapPinDto> pins = new ArrayList<>();

        for (Site site : siteRepository.findByCustomerId(principal.getCustomerId())) {
            if (site.getLatitude() != null && site.getLongitude() != null) {
                pins.add(MapPinDto.site(site.getId(), site.getName(), site.getLatitude(), site.getLongitude()));
            }
        }

        // Whoever's currently assigned to one of this customer's open work
        // orders — so the customer can see roughly where help is coming from.
        List<WorkOrder> openOrders = workOrderRepository.findByStatusNotInWithAssignee(TERMINAL);
        java.util.Set<java.util.UUID> seenTechnicians = new java.util.HashSet<>();
        for (WorkOrder wo : openOrders) {
            if (!wo.getCustomer().getId().equals(principal.getCustomerId())) continue;
            User technician = wo.getAssignedTo();
            if (technician == null || technician.getBaseLatitude() == null || technician.getBaseLongitude() == null) continue;
            if (!seenTechnicians.add(technician.getId())) continue;
            pins.add(MapPinDto.technician(technician.getId(), technician.getName(), technician.getBaseLatitude(), technician.getBaseLongitude()));
        }

        return pins;
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
