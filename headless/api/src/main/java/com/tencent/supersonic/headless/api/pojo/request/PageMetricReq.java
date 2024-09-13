package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class PageMetricReq extends PageSchemaItemReq {

    private String type;

    private Integer isTag;

    private Integer isPublish;
}
