package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit row. Never updated or deleted — Section 05 integrity rule.
 */
@Entity
@Table(name = "work_order_status_history")
public class WorkOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private WorkOrderStatus fromStatus; // null for the initial NEW row

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private WorkOrderStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(length = 500)
    private String note;

    public WorkOrderStatusHistory() {
    }

    @PrePersist
    void onCreate() {
        if (changedAt == null) changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public WorkOrderStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(WorkOrderStatus fromStatus) { this.fromStatus = fromStatus; }

    public WorkOrderStatus getToStatus() { return toStatus; }
    public void setToStatus(WorkOrderStatus toStatus) { this.toStatus = toStatus; }

    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final WorkOrderStatusHistory h = new WorkOrderStatusHistory();
        public Builder workOrder(WorkOrder workOrder) { h.workOrder = workOrder; return this; }
        public Builder fromStatus(WorkOrderStatus fromStatus) { h.fromStatus = fromStatus; return this; }
        public Builder toStatus(WorkOrderStatus toStatus) { h.toStatus = toStatus; return this; }
        public Builder changedBy(User changedBy) { h.changedBy = changedBy; return this; }
        public Builder note(String note) { h.note = note; return this; }
        public WorkOrderStatusHistory build() { return h; }
    }
}
