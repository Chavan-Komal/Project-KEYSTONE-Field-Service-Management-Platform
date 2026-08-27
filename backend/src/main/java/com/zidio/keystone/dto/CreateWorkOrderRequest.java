package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// customerId is intentionally NOT @NotNull: a CUSTOMER-role caller never sends
// one (the server forces it to their own org — see WorkOrderService.createWorkOrder),
// while a DISPATCHER/MANAGER caller must supply it. That distinction is
// enforced in the service layer rather than here, since it depends on the
// caller's role, not just the shape of the payload.
public record CreateWorkOrderRequest(
    @NotBlank String title,
    String description,
    @NotNull Priority priority,
    UUID customerId,
    @NotNull UUID siteId
) {}
