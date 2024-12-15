package com.tencent.supersonic.headless.core.translator.parser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeRange {
    private String start;
    private String end;
}
