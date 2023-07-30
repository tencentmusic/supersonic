package com.tencent.supersonic.chat.config;

import java.util.List;
import lombok.Data;

/**
 * when query an entity, return related dimension/metric info
 */
@Data
public class EntityDetailData {

    private List<Long> dimensionIds;
    private List<Long> metricIds;

}