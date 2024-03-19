package com.tencent.supersonic.headless.api.pojo.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class TagObjectReq extends SchemaItem {

    @NotNull
    private Long domainId;
    private Map<String, Object> ext = new HashMap<>();

    public String getExtJson() {
        return Objects.nonNull(ext) && ext.size() > 0 ? JSONObject.toJSONString(ext) : "";
    }
}