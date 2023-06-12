package com.tencent.supersonic.semantic.api.core.response;


import com.tencent.supersonic.semantic.api.core.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.core.pojo.MetricTypeParams;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;


@Data
public class MetricResp extends SchemaItem {

    private Long domainId;

    private String domainName;

    //measure_proxy ratio expr cumulative derived
    private String type;

    private MetricTypeParams typeParams;

    private String fullPath;


    private String dataFormatType;

    private DataFormat dataFormat;

}
