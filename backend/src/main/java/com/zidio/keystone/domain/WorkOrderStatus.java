package com.zidio.keystone.domain;

import java.util.Map;
import java.util.Set;

/**
 * The governed work-order lifecycle — Section 07 of the brief.
 * ALLOWED_TRANSITIONS is the single source of truth for which jumps are legal;
 * WorkOrderService consults this map and rejects anything not listed with a 409.
 */
public enum WorkOrderStatus {
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CLOSED,
    CANCELLED;

    public static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        NEW,          Set.of(ASSIGNED, CANCELLED),
        ASSIGNED,     Set.of(IN_PROGRESS, CANCELLED),
        IN_PROGRESS,  Set.of(ON_HOLD, COMPLETED),
        ON_HOLD,      Set.of(IN_PROGRESS),
        COMPLETED,    Set.of(CLOSED, IN_PROGRESS), // reopen
        CLOSED,       Set.of(),
        CANCELLED,    Set.of()
    );

    public boolean canTransitionTo(WorkOrderStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }
}
