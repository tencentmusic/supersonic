package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class ItemVisibilityInfo {

    private List<Long> blackDimIdList;
    private List<Long> blackMetricIdList;
    private List<Long> whiteDimIdList;
    private List<Long> whiteMetricIdList;
}
