package com.tencent.supersonic.semantic.model.domain.pojo;


import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import lombok.Data;


@Data
public class Datasource extends SchemaItem {

    private Long modelId;

    private Long databaseId;

    private ModelDetail datasourceDetail;



}
