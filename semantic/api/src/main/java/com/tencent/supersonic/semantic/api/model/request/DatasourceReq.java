package com.tencent.supersonic.semantic.api.model.request;


import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import java.util.List;

import lombok.Data;


@Data
public class DatasourceReq extends SchemaItem {

    private Long databaseId;

    private String queryType;

    private String sqlQuery;

    private String tableQuery;

    private Long modelId;

    private List<Identify> identifiers;

    private List<Dim> dimensions;

    private List<Measure> measures;


}
