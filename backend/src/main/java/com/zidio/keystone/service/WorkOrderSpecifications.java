package com.zidio.keystone.service;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Composable filters for the work-order list endpoint. Role scoping
 * (customerId / assignedTo restrictions) is composed in here too, rather than
 * trusted to the caller — the specification passed to the repository IS the
 * security boundary for what rows can even be returned from the database.
 */
public final class WorkOrderSpecifications {

    private WorkOrderSpecifications() {}

    public static Specification<WorkOrder> hasStatus(WorkOrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<WorkOrder> matchesSearch(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("code")), like),
            cb.like(cb.lower(root.get("title")), like)
        );
    }

    public static Specification<WorkOrder> forCustomer(UUID customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<WorkOrder> assignedToTechnician(UUID technicianId) {
        return (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), technicianId);
    }
}
