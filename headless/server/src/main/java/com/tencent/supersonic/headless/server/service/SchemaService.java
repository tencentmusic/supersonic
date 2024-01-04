package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.api.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.request.PageMetricReq;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.api.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.DomainResp;

import java.util.List;

public interface SchemaService {

    List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq filter, User user);

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq, User user);

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(User user, AuthType authType, Long domainId);
}
