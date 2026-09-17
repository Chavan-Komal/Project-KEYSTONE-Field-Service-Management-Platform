package com.zidio.keystone.dto;

import com.zidio.keystone.repository.WorkOrderAttachmentRepository.AttachmentMeta;

import java.time.Instant;
import java.util.UUID;

/**
 * Public shape of a work order photo — metadata only. The bytes are served
 * separately by GET /api/work-orders/{id}/attachments/{attachmentId}.
 */
public record AttachmentDto(
    UUID id,
    String filename,
    String contentType,
    long sizeBytes,
    Instant uploadedAt
) {
    public static AttachmentDto from(AttachmentMeta m) {
        return new AttachmentDto(
            m.getId(),
            m.getFilename(),
            m.getContentType(),
            m.getSizeBytes(),
            m.getUploadedAt()
        );
    }
}
