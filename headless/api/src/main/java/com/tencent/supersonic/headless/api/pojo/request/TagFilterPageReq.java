package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TagFilterPageReq extends PageSchemaItemReq {

    @NotNull
    private Long tagObjectId;

    private TagDefineType tagDefineType;
}