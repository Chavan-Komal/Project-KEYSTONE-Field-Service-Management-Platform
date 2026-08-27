package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Customer;

import java.util.UUID;

public record CustomerDto(UUID id, String name, String contactEmail) {
    public static CustomerDto from(Customer c) {
        return new CustomerDto(c.getId(), c.getName(), c.getContactEmail());
    }
}
