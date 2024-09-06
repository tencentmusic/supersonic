package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MaterializationElement {
    private List<TimeRange> timeRangeList;
    private String name;
}
