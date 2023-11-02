package com.tencent.supersonic.semantic.model.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import java.util.List;
import java.util.Map;

public interface ModelService {

    List<ModelResp> getModelListWithAuth(User user, Long domainId, AuthType authType);

    List<ModelResp> getModelAuthList(User user, AuthType authTypeEnum);

    List<ModelResp> getModelByDomainIds(List<Long> domainIds);

    List<ModelResp> getModelList();

    List<ModelResp> getModelList(List<Long> modelIds);

    ModelResp getModel(Long id);

    void updateModel(ModelReq modelReq, User user);

    void createModel(ModelReq modelReq, User user);

    void deleteModel(Long id, User user);

    Map<Long, ModelResp> getModelMap();

    Map<Long, String> getModelFullPathMap();

    List<String> getModelAdmin(Long id);

    ModelSchemaResp fetchSingleModelSchema(Long modelId);

    List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq modelSchemaFilterReq);

    DatabaseResp getDatabaseByModelId(Long modelId);
}
