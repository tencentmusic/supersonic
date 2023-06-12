package com.tencent.supersonic.chat.domain.pojo.semantic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SemanticText implements Serializable {

    private LinkedHashSet<String> metricList;

    private LinkedHashSet<String> dimensionValues = new LinkedHashSet<>();
    private Map<String, List<String>> filterDimensionValues = new HashMap<>();
    private AggregatorInfo aggregatorInfo;
    private String startTime;
    private String endTime;
    private String timeWord;
    private Integer classId;
    private String className;
}
