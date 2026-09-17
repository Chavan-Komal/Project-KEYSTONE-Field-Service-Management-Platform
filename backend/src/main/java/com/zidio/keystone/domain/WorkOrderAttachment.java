package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A photo (or other image) attached to a work order — typically a customer's
 * picture of the issue, uploaded when raising the request. Bytes are stored
 * inline as BYTEA; only image/* content types are accepted (WorkOrderService).
 */
@Entity
@Table(name = "work_order_attachments")
public class WorkOrderAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public WorkOrderAttachment() {
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final WorkOrderAttachment a = new WorkOrderAttachment();
        public Builder workOrder(WorkOrder workOrder) { a.workOrder = workOrder; return this; }
        public Builder filename(String filename) { a.filename = filename; return this; }
        public Builder contentType(String contentType) { a.contentType = contentType; return this; }
        public Builder sizeBytes(long sizeBytes) { a.sizeBytes = sizeBytes; return this; }
        public Builder data(byte[] data) { a.data = data; return this; }
        public Builder uploadedBy(User uploadedBy) { a.uploadedBy = uploadedBy; return this; }
        public WorkOrderAttachment build() { return a; }
    }
}
