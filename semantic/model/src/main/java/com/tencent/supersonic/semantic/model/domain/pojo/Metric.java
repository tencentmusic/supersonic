package com.tencent.supersonic.semantic.model.domain.pojo;


import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;

@Data
public class Metric extends SchemaItem {


    private Long modelId;

    //measure_proxy ratio expr cumulative derived
    private String type;

    private MetricTypeParams typeParams;

    private String dataFormatType;

    private DataFormat dataFormat;

    private String alias;

}
