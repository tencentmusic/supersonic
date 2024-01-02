package com.tencent.supersonic.chat.api.component;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.request.PageMetricReq;
import com.tencent.supersonic.headless.api.response.DomainResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.ExplainResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.api.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.api.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.request.QueryS2SQLReq;
import com.tencent.supersonic.headless.api.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;

import java.util.List;

/**
 * A semantic layer provides a simplified and consistent view of data from multiple sources.
 * It abstracts away the complexity of the underlying data sources and provides a unified view
 * of the data that is easier to understand and use.
 * <p>
 * The interface defines methods for getting metadata as well as querying data in the semantic layer.
 * Implementations of this interface should provide concrete implementations that interact with the
 * underlying data sources and return results in a consistent format. Or it can be implemented
 * as proxy to a remote semantic service.
 * </p>
 */
public interface SemanticInterpreter {

    QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user);

    QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user);

    QueryResultWithSchemaResp queryByS2SQL(QueryS2SQLReq queryS2SQLReq, User user);

    QueryResultWithSchemaResp queryDimValue(QueryDimValueReq queryDimValueReq, User user);

    List<ModelSchema> getModelSchema();

    List<ModelSchema> getModelSchema(List<Long> ids);

    ModelSchema getModelSchema(Long model, Boolean cacheEnable);

    PageInfo<DimensionResp> getDimensionPage(PageDimensionReq pageDimensionReq);

    PageInfo<MetricResp> getMetricPage(PageMetricReq pageDimensionReq, User user);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(AuthType authType, Long domainId, User user);

    <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception;

    List<ModelSchemaResp> fetchModelSchema(List<Long> ids, Boolean cacheEnable);

}
