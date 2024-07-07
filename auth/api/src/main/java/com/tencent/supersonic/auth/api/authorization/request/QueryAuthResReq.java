package com.tencent.supersonic.auth.api.authorization.request;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class QueryAuthResReq {

    private List<String> departmentIds = new ArrayList<>();

    private Long modelId;

    private List<Long> modelIds;

    public List<Long> getModelIds() {
        if (!CollectionUtils.isEmpty(modelIds)) {
            return modelIds;
        }
        if (modelId != null) {
            return Lists.newArrayList(modelId);
        }
        return Lists.newArrayList();
    }
}
