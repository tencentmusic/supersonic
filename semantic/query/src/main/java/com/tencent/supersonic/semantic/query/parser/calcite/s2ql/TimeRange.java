package com.tencent.supersonic.semantic.query.parser.calcite.s2ql;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeRange {
    private String start;
    private String end;
}
