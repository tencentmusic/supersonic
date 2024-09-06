package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

@Data
public class TagFilterPageReq extends PageSchemaItemReq {

    private Long tagObjectId;

    private TagDefineType tagDefineType;
}
