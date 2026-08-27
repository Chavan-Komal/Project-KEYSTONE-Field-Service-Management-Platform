package com.zidio.keystone.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A deliberately slim page wrapper — Spring Data's own Page<T> serialises with
 * a lot of extra pageable/sort metadata that the SPA doesn't need. This keeps
 * list endpoint payloads matching exactly what the frontend's Page<T> type expects.
 */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }
}
