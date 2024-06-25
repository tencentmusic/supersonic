package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

@Data
public class MetricFilter extends MetaFilter {

    private String type;

    private String userName;

    private Integer isPublish;
}
