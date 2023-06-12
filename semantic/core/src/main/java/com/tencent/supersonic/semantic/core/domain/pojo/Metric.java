package com.tencent.supersonic.semantic.core.domain.pojo;


import com.tencent.supersonic.semantic.api.core.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.core.pojo.MetricTypeParams;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;

@Data
public class Metric extends SchemaItem {


    private Long domainId;

    //measure_proxy ratio expr cumulative derived
    private String type;

    private MetricTypeParams typeParams;

    private String dataFormatType;

    private DataFormat dataFormat;

}
