package com.zidio.keystone.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PartUsageRequest(
    @NotNull UUID partId,
    @Min(1) int qtyUsed
) {}
