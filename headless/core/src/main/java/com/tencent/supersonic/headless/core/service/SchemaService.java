package com.tencent.supersonic.headless.core.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.common.server.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.common.server.request.PageDimensionReq;
import com.tencent.supersonic.headless.common.server.request.PageMetricReq;
import com.tencent.supersonic.headless.common.server.response.ModelResp;
import com.tencent.supersonic.headless.common.server.response.ModelSchemaResp;
import com.tencent.supersonic.headless.common.server.response.DimensionResp;
import com.tencent.supersonic.headless.common.server.response.MetricResp;
import com.tencent.supersonic.headless.common.server.response.DomainResp;

import java.util.List;

public interface SchemaService {

    List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq filter, User user);

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq, User user);

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(User user, AuthType authType, Long domainId);
}
