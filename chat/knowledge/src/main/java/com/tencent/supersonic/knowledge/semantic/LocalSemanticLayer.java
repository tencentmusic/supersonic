package com.tencent.supersonic.knowledge.semantic;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.S2ThreadContext;
import com.tencent.supersonic.common.util.ThreadContext;
import com.tencent.supersonic.semantic.api.model.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.query.service.QueryService;
import com.tencent.supersonic.semantic.query.service.SchemaService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalSemanticLayer extends BaseSemanticLayer {

    private SchemaService schemaService;
    private S2ThreadContext s2ThreadContext;
    private DomainService domainService;
    private DimensionService dimensionService;
    private MetricService metricService;

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user) {
        try {
            QueryService queryService = ContextUtils.getBean(QueryService.class);
            QueryResultWithSchemaResp queryResultWithSchemaResp = queryService.queryByStruct(queryStructReq, user);
            return queryResultWithSchemaResp;
        } catch (Exception e) {
            log.info("queryByStruct has an exception:{}", e.toString());
        }
        return null;
    }

    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user) {
        try {
            QueryService queryService = ContextUtils.getBean(QueryService.class);
            return queryService.queryByMultiStruct(queryMultiStructReq, user);
        } catch (Exception e) {
            log.info("queryByMultiStruct has an exception:{}", e);
        }
        return null;
    }

    @Override
    public QueryResultWithSchemaResp queryByDsl(QueryDslReq queryDslReq, User user) {
        try {
            QueryService queryService = ContextUtils.getBean(QueryService.class);
            Object object = queryService.queryBySql(queryDslReq, user);
            QueryResultWithSchemaResp queryResultWithSchemaResp = JsonUtil.toObject(JsonUtil.toString(object),
                    QueryResultWithSchemaResp.class);
            return queryResultWithSchemaResp;
        } catch (Exception e) {
            log.info("queryByDsl has an exception:{}", e);
        }
        return null;
    }

    @Override
    public List<DomainSchemaResp> doFetchDomainSchema(List<Long> ids) {
        DomainSchemaFilterReq filter = new DomainSchemaFilterReq();
        filter.setDomainIds(ids);
        User user = new User(1L, "admin", "admin", "admin@email");
        schemaService = ContextUtils.getBean(SchemaService.class);
        return schemaService.fetchDomainSchema(filter, user);
    }
    @Override
    public List<DomainResp> getDomainListForViewer() {
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        ThreadContext threadContext = s2ThreadContext.get();
        domainService = ContextUtils.getBean(DomainService.class);
        return domainService.getDomainListForViewer(threadContext.getUserName());
    }

    @Override
    public List<DomainResp> getDomainListForAdmin() {
        domainService = ContextUtils.getBean(DomainService.class);
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        ThreadContext threadContext = s2ThreadContext.get();
        return domainService.getDomainListForAdmin(threadContext.getUserName());
    }

    @Override
    public PageInfo<DimensionResp> getDimensionPage(PageDimensionReq pageDimensionCmd) {
        dimensionService = ContextUtils.getBean(DimensionService.class);
        return dimensionService.queryDimension(pageDimensionCmd);
    }

    @Override
    public PageInfo<MetricResp> getMetricPage(PageMetricReq pageMetricCmd) {
        metricService = ContextUtils.getBean(MetricService.class);
        return metricService.queryMetric(pageMetricCmd);
    }

}
