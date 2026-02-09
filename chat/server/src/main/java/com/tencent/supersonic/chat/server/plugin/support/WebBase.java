package com.tencent.supersonic.chat.server.plugin.support;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WebBase {

    private String url;

    /** HTTP method: GET, POST, PUT, DELETE. Defaults to POST for backward compatibility. */
    private String method = "POST";

    /** Request timeout in milliseconds. Defaults to 30 seconds. */
    private Integer timeoutMs = 30000;

    /** Custom headers to include in the request. */
    private Map<String, String> headers;

    /** Response data path using JSONPath syntax, e.g. "$.data.items" */
    private String responsePath;

    private List<ParamOption> paramOptions = Lists.newArrayList();

    public List<ParamOption> getParams() {
        return paramOptions;
    }
}
