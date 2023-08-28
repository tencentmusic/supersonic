package com.tencent.supersonic.chat.api.pojo.response;

import java.util.List;
import lombok.Data;


@Data
public class ItemVisibilityInfo {

    private List<Long> blackDimIdList;
    private List<Long> blackMetricIdList;
    private List<Long> whiteDimIdList;
    private List<Long> whiteMetricIdList;
}