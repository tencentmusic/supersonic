package com.tencent.supersonic.headless.api.service;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.*;
import com.tencent.supersonic.headless.api.pojo.schema.DataModelSchema;
import com.tencent.supersonic.headless.api.pojo.schema.DimensionSchema;
import com.tencent.supersonic.headless.api.pojo.schema.MetricSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public interface SchemaService {

    DataSetSchema getDataSetSchema(Long dataSetId);

    SemanticSchema getSemanticSchema();

    SemanticSchema getSemanticSchema(Set<Long> dataSetIds);

    SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq);

    List<ModelSchemaResp> fetchModelSchemaResps(List<Long> modelIds);

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    DimensionResp getDimension(String bizName, Long modelId);

    DimensionResp getDimension(Long id);

    List<MetricResp> getMetrics(MetaFilter metaFilter);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(User user, AuthType authType, Long domainId);

    List<ModelResp> getModelList(List<Long> modelIds);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) throws ExecutionException;

    List<ItemResp> getDomainDataSetTree();

    void buildSchemaDocuments(SemanticSchemaResp semanticSchemaResp,
            Map<String, List<DimensionSchema>> dimensionSchemaMap,
            List<DataModelSchema> dataModelSchemas, List<MetricSchema> metricSchemas,
            Map<Long, String> modelIdName);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

}
