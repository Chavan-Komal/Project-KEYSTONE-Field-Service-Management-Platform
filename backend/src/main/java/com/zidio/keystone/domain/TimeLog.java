package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_logs")
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(nullable = false)
    private int minutes;

    @Column(length = 500)
    private String note;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    public TimeLog() {
    }

    @PrePersist
    void onCreate() {
        if (loggedAt == null) loggedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public User getTechnician() { return technician; }
    public void setTechnician(User technician) { this.technician = technician; }

    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final TimeLog t = new TimeLog();
        public Builder workOrder(WorkOrder workOrder) { t.workOrder = workOrder; return this; }
        public Builder technician(User technician) { t.technician = technician; return this; }
        public Builder minutes(int minutes) { t.minutes = minutes; return this; }
        public Builder note(String note) { t.note = note; return this; }
        public TimeLog build() { return t; }
    }
}
