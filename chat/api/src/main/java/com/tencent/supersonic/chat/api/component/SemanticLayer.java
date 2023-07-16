package com.tencent.supersonic.chat.api.component;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.*;
import com.tencent.supersonic.semantic.api.query.request.QuerySqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;

import java.util.List;

/**
 * This interface defines the contract for a semantic layer that provides a simplified and
 * consistent view of data from multiple sources.
 * The semantic layer abstracts away the complexity of the underlying data sources and provides
 * a unified view of the data that is easier to understand and use.
 * <p>
 * The interface defines methods for getting metadata as well as querying data in the semantic layer.
 * Implementations of this interface should provide concrete implementations that interact with the
 * underlying data sources and return results in a consistent format. Or it can be implemented
 * as proxy to a remote semantic service.
 * </p>
 */
public interface SemanticLayer {

    QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user);

    QueryResultWithSchemaResp queryBySql(QuerySqlReq querySqlReq, User user);

    DomainSchemaResp getDomainSchemaInfo(Long domain, Boolean cacheEnable);

    List<DomainSchemaResp> getDomainSchemaInfo(List<Long> ids);

    List<DomainResp> getDomainListForViewer();

    List<DomainResp> getDomainListForAdmin();

    PageInfo<DimensionResp> queryDimensionPage(PageDimensionReq pageDimensionCmd);

    PageInfo<MetricResp> queryMetricPage(PageMetricReq pageMetricCmd);

//    PageInfo<MetricResp> queryMetricPage(PageMetricReq pageMetricCmd);
//
//    PageInfo<DimensionResp> queryDimensionPage(PageDimensionReq pageDimensionCmd);
//
//    List<DomainResp> getDomainListForAdmin();
//
//    List<DomainResp> getDomainListForViewer();

}
