package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Site;

import java.util.UUID;

public record SiteDto(UUID id, UUID customerId, String name, String address) {
    public static SiteDto from(Site s) {
        return new SiteDto(s.getId(), s.getCustomer().getId(), s.getName(), s.getAddress());
    }
}
