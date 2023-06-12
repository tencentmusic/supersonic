package com.tencent.supersonic.semantic.core.domain.pojo;


import com.tencent.supersonic.semantic.api.core.pojo.DatasourceDetail;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;


@Data
public class Datasource extends SchemaItem {

    private Long domainId;

    private Long databaseId;

    private DatasourceDetail datasourceDetail;


}
