package com.tencent.supersonic.semantic.api.core.request;


import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;


@Data
public class MetricBaseReq extends SchemaItem {

    private Long domainId;

}
