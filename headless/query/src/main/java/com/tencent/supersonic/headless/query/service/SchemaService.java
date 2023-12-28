package com.tencent.supersonic.headless.query.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.common.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.common.model.request.PageDimensionReq;
import com.tencent.supersonic.headless.common.model.request.PageMetricReq;
import com.tencent.supersonic.headless.common.model.response.ModelResp;
import com.tencent.supersonic.headless.common.model.response.ModelSchemaResp;
import com.tencent.supersonic.headless.common.model.response.DimensionResp;
import com.tencent.supersonic.headless.common.model.response.MetricResp;
import com.tencent.supersonic.headless.common.model.response.DomainResp;

import java.util.List;

public interface SchemaService {

    List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq filter, User user);

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq, User user);

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(User user, AuthType authType, Long domainId);
}
