package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.*;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.UnAvailableItemResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DimensionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ModelService {

    ModelResp createModel(ModelReq datasourceReq, User user) throws Exception;

    List<ModelResp> createModel(ModelBuildReq modelBuildReq, User user) throws Exception;

    ModelResp updateModel(ModelReq datasourceReq, User user) throws Exception;

    List<ModelResp> getModelList(MetaFilter metaFilter);

    Map<Long, ModelResp> getModelMap(ModelFilter modelFilter);

    void deleteModel(Long id, User user);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    UnAvailableItemResp getUnAvailableItem(FieldRemovedReq fieldRemovedReq);

    Map<String, ModelSchema> buildModelSchema(ModelBuildReq modelBuildReq) throws SQLException;

    List<ModelResp> getModelListWithAuth(User user, Long domainId, AuthType authType);

    List<ModelResp> getModelAuthList(User user, Long domainId, AuthType authTypeEnum);

    List<ModelResp> getModelByDomainIds(List<Long> domainIds);

    List<ModelResp> getAllModelByDomainIds(List<Long> domainIds);

    ModelResp getModel(Long id);

    List<String> getModelAdmin(Long id);

    DatabaseResp getDatabaseByModelId(Long modelId);

    void batchUpdateStatus(MetaBatchReq metaBatchReq, User user);

    void updateModelByDimAndMetric(Long modelId, List<DimensionReq> dimensionReqList, List<MetricReq> metricReqList, User user);

    void deleteModelDetailByDimAndMetric(Long modelId, List<DimensionDO> dimensionReqList, List<MetricDO> metricReqList);
}
