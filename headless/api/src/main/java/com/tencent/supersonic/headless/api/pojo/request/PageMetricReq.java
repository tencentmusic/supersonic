package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PageMetricReq extends PageSchemaItemReq {

    private String type;

    private Integer isTag;

    private Integer isPublish;
}
