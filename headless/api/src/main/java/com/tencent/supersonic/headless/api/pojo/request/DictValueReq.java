package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DictValueReq extends PageBaseReq {
    private Long modelId;

    private Long itemId;

    private TypeEnums type = TypeEnums.DIMENSION;

    private String keyValue;
}
