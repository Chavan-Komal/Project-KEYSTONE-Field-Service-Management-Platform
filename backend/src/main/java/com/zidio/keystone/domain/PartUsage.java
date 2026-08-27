package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "part_usage")
public class PartUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(name = "qty_used", nullable = false)
    private int qtyUsed;

    @Column(name = "unit_cost_at_use", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCostAtUse;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    public PartUsage() {
    }

    @PrePersist
    void onCreate() {
        if (loggedAt == null) loggedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }

    public int getQtyUsed() { return qtyUsed; }
    public void setQtyUsed(int qtyUsed) { this.qtyUsed = qtyUsed; }

    public BigDecimal getUnitCostAtUse() { return unitCostAtUse; }
    public void setUnitCostAtUse(BigDecimal unitCostAtUse) { this.unitCostAtUse = unitCostAtUse; }

    public Instant getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final PartUsage p = new PartUsage();
        public Builder workOrder(WorkOrder workOrder) { p.workOrder = workOrder; return this; }
        public Builder part(Part part) { p.part = part; return this; }
        public Builder qtyUsed(int qtyUsed) { p.qtyUsed = qtyUsed; return this; }
        public Builder unitCostAtUse(BigDecimal unitCostAtUse) { p.unitCostAtUse = unitCostAtUse; return this; }
        public PartUsage build() { return p; }
    }
}
