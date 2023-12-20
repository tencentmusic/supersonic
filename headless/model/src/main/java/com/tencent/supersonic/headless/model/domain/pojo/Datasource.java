package com.tencent.supersonic.headless.model.domain.pojo;


import com.tencent.supersonic.headless.api.model.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.model.pojo.SchemaItem;
import lombok.Data;


@Data
public class Datasource extends SchemaItem {

    private Long modelId;

    private Long databaseId;

    private ModelDetail datasourceDetail;

    private String sourceType;



}
