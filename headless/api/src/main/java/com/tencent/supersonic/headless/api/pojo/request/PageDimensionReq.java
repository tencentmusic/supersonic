package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PageDimensionReq extends PageSchemaItemReq {

    private Integer isTag;
}
