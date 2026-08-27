package com.zidio.keystone.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSiteRequest(
    @NotBlank String name,
    @NotBlank String address
) {}
