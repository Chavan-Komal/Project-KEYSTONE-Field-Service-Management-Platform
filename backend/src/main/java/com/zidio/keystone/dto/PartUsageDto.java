package com.zidio.keystone.dto;

import com.zidio.keystone.domain.PartUsage;

import java.math.BigDecimal;
import java.util.UUID;

public record PartUsageDto(UUID id, UUID partId, String partName, int qtyUsed, BigDecimal unitCost) {
    public static PartUsageDto from(PartUsage p) {
        return new PartUsageDto(p.getId(), p.getPart().getId(), p.getPart().getName(), p.getQtyUsed(), p.getUnitCostAtUse());
    }
}
