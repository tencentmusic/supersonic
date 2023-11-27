package com.tencent.supersonic.semantic.model.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.request.MetaBatchReq;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.yaml.DataModelYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.model.domain.pojo.ModelFilter;
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
