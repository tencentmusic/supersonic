package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Represents a pending confirmation action waiting for user approval.
 */
@Data
@Builder
public class PendingConfirmation {
    private Long dataSetId;
    private ScheduleIntent intent;
    private Map<String, Object> params;
    private long createdAt;
    private long expireAt;

    /**
     * Check if this confirmation has expired.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }
}
