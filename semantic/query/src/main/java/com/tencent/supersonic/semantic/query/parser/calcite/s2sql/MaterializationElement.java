package com.tencent.supersonic.semantic.query.parser.calcite.s2sql;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterializationElement {
    private List<TimeRange> timeRangeList;
    private String name;
}
