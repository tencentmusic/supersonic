package com.tencent.supersonic.headless.server.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PermissionSnapshot {
    private Long userId;
    private List<String> authorizedDimensions;
    private Map<String, List<String>> dimensionFilters;
    private List<String> maskedColumns;
}
