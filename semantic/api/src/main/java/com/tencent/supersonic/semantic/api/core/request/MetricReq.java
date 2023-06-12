package com.tencent.supersonic.semantic.api.core.request;


import com.tencent.supersonic.semantic.api.core.pojo.MetricTypeParams;
import lombok.Data;

@Data
public class MetricReq extends MetricBaseReq {

    private MetricTypeParams typeParams;

}
