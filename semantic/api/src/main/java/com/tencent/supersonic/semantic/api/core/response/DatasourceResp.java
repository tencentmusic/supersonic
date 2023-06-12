package com.tencent.supersonic.semantic.api.core.response;

import com.tencent.supersonic.semantic.api.core.pojo.DatasourceDetail;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;

@Data
public class DatasourceResp extends SchemaItem {

    private Long domainId;

    private Long databaseId;

    private DatasourceDetail datasourceDetail;


}
