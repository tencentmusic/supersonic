package com.tencent.supersonic.headless.server.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutputConfig {
    private OutputFormat format;
    private Boolean async;
}
