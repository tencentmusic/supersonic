package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resource quota defined by a subscription plan. A value of -1 means unlimited.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanQuota {

    private String planName;

    private Integer maxUsers;

    private Integer maxDatasets;

    private Integer maxModels;

    private Integer maxAgents;

    private Integer maxApiCallsPerDay;

    private Long maxTokensPerMonth;

    public boolean isUnlimited(Integer limit) {
        return limit == null || limit == -1;
    }

    public boolean isUnlimited(Long limit) {
        return limit == null || limit == -1L;
    }
}
