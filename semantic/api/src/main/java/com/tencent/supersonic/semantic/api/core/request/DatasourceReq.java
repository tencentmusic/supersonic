package com.tencent.supersonic.semantic.api.core.request;


import com.tencent.supersonic.semantic.api.core.pojo.Dim;
import com.tencent.supersonic.semantic.api.core.pojo.Identify;
import com.tencent.supersonic.semantic.api.core.pojo.Measure;
import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.List;

import lombok.Data;


@Data
public class DatasourceReq extends SchemaItem {

    private Long databaseId;

    private String queryType;

    private String sqlQuery;

    private String sqlTable;

    private Long domainId;

    private List<Identify> identifiers;

    private List<Dim> dimensions;

    private List<Measure> measures;


}
