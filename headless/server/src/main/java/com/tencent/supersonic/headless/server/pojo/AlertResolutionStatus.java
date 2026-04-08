package com.tencent.supersonic.headless.server.pojo;

import java.util.Set;

public enum AlertResolutionStatus {
    OPEN(Set.of("CONFIRMED", "ASSIGNED")),
    CONFIRMED(Set.of("ASSIGNED", "RESOLVED")),
    ASSIGNED(Set.of("RESOLVED")),
    RESOLVED(Set.of("CLOSED")),
    CLOSED(Set.of());

    private final Set<String> allowedTargets;

    AlertResolutionStatus(Set<String> allowedTargets) {
        this.allowedTargets = allowedTargets;
    }

    public boolean canTransitionTo(AlertResolutionStatus target) {
        return allowedTargets.contains(target.name());
    }
}
