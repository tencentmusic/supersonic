package com.tencent.supersonic.headless.core.translator.parser;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MaterializationElement {
    private List<TimeRange> timeRangeList;
    private String name;
}
