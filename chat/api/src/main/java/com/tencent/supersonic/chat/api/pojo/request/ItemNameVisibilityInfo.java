package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemNameVisibilityInfo {

    /** invisible dimensions */
    private List<String> blackDimNameList = new ArrayList<>();

    /** invisible metrics */
    private List<String> blackMetricNameList = new ArrayList<>();
}
