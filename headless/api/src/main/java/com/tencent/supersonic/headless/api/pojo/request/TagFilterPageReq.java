package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TagFilterPageReq extends PageSchemaItemReq {

    private Long tagObjectId;

    private TagDefineType tagDefineType;
}
