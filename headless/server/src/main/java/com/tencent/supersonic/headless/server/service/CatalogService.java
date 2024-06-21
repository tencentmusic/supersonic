package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface CatalogService {

    DimensionResp getDimension(String bizName, Long modelId);

    DimensionResp getDimension(Long id);

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    List<MetricResp> getMetrics(MetaFilter metaFilter);

    MetricResp getMetric(Long id);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    List<ModelResp> getModelList(List<Long> modelIds);

    void getSchemaYamlTpl(SemanticSchemaResp semanticSchemaResp,
                            Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
                            List<DataModelYamlTpl> dataModelYamlTplList,
                            List<MetricYamlTpl> metricYamlTplList,
                            Map<Long, String> modelIdName);

    SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) throws ExecutionException;
}
