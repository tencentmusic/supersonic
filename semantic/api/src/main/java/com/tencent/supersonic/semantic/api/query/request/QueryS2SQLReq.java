package com.tencent.supersonic.semantic.api.query.request;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.ToString;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@ToString
public class QueryS2SQLReq {

    private Set<Long> modelIds;

    private String sql;

    private Map<String, String> variables;

    public void setModelId(Long modelId) {
        modelIds = new HashSet<>();
        modelIds.add(modelId);
    }

    public List<Long> getModelIds() {
        return Lists.newArrayList(modelIds);
    }

}
