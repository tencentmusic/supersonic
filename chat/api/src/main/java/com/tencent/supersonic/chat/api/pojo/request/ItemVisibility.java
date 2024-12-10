package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemVisibility {

    /** invisible dimensions */
    private List<Long> blackDimIdList = new ArrayList<>();

    /** invisible metrics */
    private List<Long> blackMetricIdList = new ArrayList<>();
}
