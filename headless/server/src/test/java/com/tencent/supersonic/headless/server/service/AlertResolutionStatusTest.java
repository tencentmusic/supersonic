package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.AlertResolutionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertResolutionStatusTest {

    @Test
    void openCanTransitionToConfirmedOrAssigned() {
        assertTrue(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.CONFIRMED));
        assertTrue(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.ASSIGNED));
        assertFalse(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.RESOLVED));
        assertFalse(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.CLOSED));
    }

    @Test
    void confirmedCanTransitionToAssignedOrResolved() {
        assertTrue(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.ASSIGNED));
        assertTrue(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.RESOLVED));
        assertFalse(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.CLOSED));
    }

    @Test
    void assignedCanTransitionToResolved() {
        assertTrue(AlertResolutionStatus.ASSIGNED.canTransitionTo(AlertResolutionStatus.RESOLVED));
        assertFalse(AlertResolutionStatus.ASSIGNED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.ASSIGNED.canTransitionTo(AlertResolutionStatus.CLOSED));
    }

    @Test
    void resolvedCanTransitionToClosed() {
        assertTrue(AlertResolutionStatus.RESOLVED.canTransitionTo(AlertResolutionStatus.CLOSED));
        assertFalse(AlertResolutionStatus.RESOLVED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.RESOLVED.canTransitionTo(AlertResolutionStatus.ASSIGNED));
    }

    @Test
    void closedIsTerminal() {
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.CONFIRMED));
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.ASSIGNED));
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.RESOLVED));
    }
}
