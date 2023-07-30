package com.tencent.supersonic.semantic.api.model.response;


import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;


@Data
@ToString(callSuper = true)
public class MetricResp extends SchemaItem {

    private Long domainId;

    private String domainName;

    //ATOMIC DERIVED
    private String type;

    private MetricTypeParams typeParams;

    private String fullPath;

    private String dataFormatType;

    private DataFormat dataFormat;

    private String alias;


}
