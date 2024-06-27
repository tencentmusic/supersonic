package com.tencent.supersonic.headless.server.web.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface SchemaService {

    DataSetSchema getDataSetSchema(Long dataSetId);

    SemanticSchema getSemanticSchema();

    SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq);

    List<ModelSchemaResp> fetchModelSchemaResps(List<Long> modelIds);

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    DimensionResp getDimension(String bizName, Long modelId);

    List<MetricResp> getMetrics(MetaFilter metaFilter);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(User user, AuthType authType, Long domainId);

    List<ModelResp> getModelList(List<Long> modelIds);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) throws ExecutionException;

    List<ItemResp> getDomainDataSetTree();

    void getSchemaYamlTpl(SemanticSchemaResp semanticSchemaResp,
                          Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
                          List<DataModelYamlTpl> dataModelYamlTplList,
                          List<MetricYamlTpl> metricYamlTplList,
                          Map<Long, String> modelIdName);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    DatabaseResp getDatabase(Long id);
}
