package com.tencent.supersonic.semantic.api.model.response;

import com.tencent.supersonic.semantic.api.model.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;

@Data
public class DatasourceResp extends SchemaItem {

    private Long modelId;

    private Long databaseId;

    private DatasourceDetail datasourceDetail;



}
