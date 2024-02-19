package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.MetricTable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ViewQueryParam {

    private Map<String, String> variables;
    private String sql = "";
    private List<MetricTable> tables;
    private boolean supportWith = true;
    private boolean withAlias = true;

    public Map<String, String> getVariables() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        return variables;
    }
}
