package com.tencent.supersonic.chat.api.pojo.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AggregateInfo {
    private List<MetricInfo> metricInfos = new ArrayList<>();
}
