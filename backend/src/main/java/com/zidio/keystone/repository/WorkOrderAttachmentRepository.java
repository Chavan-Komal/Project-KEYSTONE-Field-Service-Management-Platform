package com.zidio.keystone.repository;

import com.zidio.keystone.domain.WorkOrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WorkOrderAttachmentRepository extends JpaRepository<WorkOrderAttachment, UUID> {

    /**
     * Closed projection — deliberately omits the {@code data} column so listing
     * a work order's photos never pulls megabytes of image bytes into memory.
     * The bytes are fetched one at a time via findById when an image is served.
     */
    interface AttachmentMeta {
        UUID getId();
        String getFilename();
        String getContentType();
        long getSizeBytes();
        Instant getUploadedAt();
    }

    List<AttachmentMeta> findByWorkOrderIdOrderByUploadedAtAsc(UUID workOrderId);
}
