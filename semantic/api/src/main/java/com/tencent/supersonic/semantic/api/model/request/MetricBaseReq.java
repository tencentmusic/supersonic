package com.tencent.supersonic.semantic.api.model.request;

import com.tencent.supersonic.semantic.api.model.pojo.RelateDimension;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.common.pojo.DataFormat;
import lombok.Data;
import java.util.List;


@Data
public class MetricBaseReq extends SchemaItem {

    private Long modelId;

    private String alias;

    private String dataFormatType;

    private DataFormat dataFormat;

    private List<String> tags;

    private RelateDimension relateDimension;

}
