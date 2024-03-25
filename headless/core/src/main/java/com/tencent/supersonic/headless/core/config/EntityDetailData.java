package com.tencent.supersonic.headless.core.config;

import lombok.Data;

import java.util.List;

/**
 * when query an entity, return related dimension/metric info
 */
@Data
public class EntityDetailData {

    private List<Long> dimensionIds;
    private List<Long> metricIds;

}