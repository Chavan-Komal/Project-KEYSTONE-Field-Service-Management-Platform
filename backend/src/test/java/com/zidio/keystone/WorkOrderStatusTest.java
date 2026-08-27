package com.zidio.keystone;

import com.zidio.keystone.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import static com.zidio.keystone.domain.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Per Section 16.1: "tests where they matter most — the lifecycle transitions
 * and the authorisation rules." This covers the lifecycle half; authorisation
 * is exercised via WorkOrderService integration tests (add as the backend grows).
 */
class WorkOrderStatusTest {

    @Test
    void newCanMoveToAssignedOrCancelled() {
        assertTrue(NEW.canTransitionTo(ASSIGNED));
        assertTrue(NEW.canTransitionTo(CANCELLED));
        assertFalse(NEW.canTransitionTo(COMPLETED));
        assertFalse(NEW.canTransitionTo(CLOSED));
    }

    @Test
    void illegalJumpFromNewToCompletedIsRejected() {
        assertFalse(NEW.canTransitionTo(COMPLETED));
    }

    @Test
    void inProgressCanHoldOrComplete() {
        assertTrue(IN_PROGRESS.canTransitionTo(ON_HOLD));
        assertTrue(IN_PROGRESS.canTransitionTo(COMPLETED));
        assertFalse(IN_PROGRESS.canTransitionTo(NEW));
    }

    @Test
    void completedCanCloseOrReopen() {
        assertTrue(COMPLETED.canTransitionTo(CLOSED));
        assertTrue(COMPLETED.canTransitionTo(IN_PROGRESS));
    }

    @Test
    void terminalStatesCannotTransitionFurther() {
        assertTrue(CLOSED.isTerminal());
        assertTrue(CANCELLED.isTerminal());
        for (WorkOrderStatus target : values()) {
            assertFalse(CLOSED.canTransitionTo(target));
            assertFalse(CANCELLED.canTransitionTo(target));
        }
    }
}
