package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Map;

@Data
public class ReportExecuteReq {
    private Long datasetId;
    private String queryConfig;
    private Map<String, Object> params;
    private OutputFormat outputFormat;
}
