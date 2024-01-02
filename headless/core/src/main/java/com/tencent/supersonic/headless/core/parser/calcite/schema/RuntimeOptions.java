package com.tencent.supersonic.headless.core.parser.calcite.schema;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;

@Data
@Builder
public class RuntimeOptions {
    private Triple<String, String, String> minMaxTime;
    private Boolean enableOptimize;

}
