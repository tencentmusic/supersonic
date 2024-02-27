package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.TagDefineParams;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class TagResp extends SchemaItem {

    private Long modelId;

    private String type;

    private Map<String, Object> ext = new HashMap<>();

    private TagDefineType tagDefineType = TagDefineType.FIELD;

    private TagDefineParams tagDefineParams;

    public String getExpr() {
        return tagDefineParams.getExpr();
    }

}
