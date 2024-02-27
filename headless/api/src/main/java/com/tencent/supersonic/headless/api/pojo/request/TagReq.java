package com.tencent.supersonic.headless.api.pojo.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.TagDefineParams;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.enums.TagType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Data;

@Data
public class TagReq extends SchemaItem {

    private Long modelId;
    private Map<String, Object> ext = new HashMap<>();
    private TagDefineType tagDefineType;
    private TagDefineParams tagDefineParams;

    public String getTypeParamsJson() {
        return JSONObject.toJSONString(tagDefineParams);
    }

    public String getExtJson() {
        return Objects.nonNull(ext) && ext.size() > 0 ? JSONObject.toJSONString(ext) : "";
    }

    public TagType getType() {
        return TagType.getType(tagDefineType);
    }

}
