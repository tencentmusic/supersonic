package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.common.server.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.common.server.request.MetaBatchReq;
import com.tencent.supersonic.headless.common.server.request.ModelReq;
import com.tencent.supersonic.headless.common.server.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.common.server.response.DatabaseResp;
import com.tencent.supersonic.headless.common.server.response.MeasureResp;
import com.tencent.supersonic.headless.common.server.response.ModelResp;
import com.tencent.supersonic.headless.common.server.response.ModelSchemaResp;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ModelService {

    ModelResp createModel(ModelReq datasourceReq, User user) throws Exception;

    ModelResp updateModel(ModelReq datasourceReq, User user) throws Exception;

    List<ModelResp> getModelList(ModelFilter modelFilter);

    Map<Long, ModelResp> getModelMap();

    void deleteModel(Long id, User user);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    List<MeasureResp> getMeasureListOfModel(List<Long> modelIds);

    List<ModelResp> getModelListWithAuth(User user, Long domainId, AuthType authType);

    List<ModelResp> getModelAuthList(User user, AuthType authTypeEnum);

    List<ModelResp> getModelByDomainIds(List<Long> domainIds);

    ModelResp getModel(Long id);

    List<String> getModelAdmin(Long id);

    ModelSchemaResp fetchSingleModelSchema(Long modelId);

    List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq modelSchemaFilterReq);

    DatabaseResp getDatabaseByModelId(Long modelId);

    void batchUpdateStatus(MetaBatchReq metaBatchReq, User user);

    void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName);

}
