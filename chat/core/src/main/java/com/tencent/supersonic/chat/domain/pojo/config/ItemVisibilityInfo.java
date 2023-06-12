package com.tencent.supersonic.chat.domain.pojo.config;

import java.util.List;
import lombok.Data;


@Data
public class ItemVisibilityInfo {

    private List<Long> blackDimIdList;
    private List<Long> blackMetricIdList;
    private List<Long> whiteDimIdList;
    private List<Long> whiteMetricIdList;
}