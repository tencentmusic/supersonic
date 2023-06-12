package com.tencent.supersonic.chat.domain.pojo.config;

import java.util.List;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ItemVisibility {

    /**
     * invisible dimensions
     */
    private List<Long> blackDimIdList;

    /**
     * invisible metrics
     */
    private List<Long> blackMetricIdList;
}