package com.tencent.supersonic.knowledge.semantic;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ExplainSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryDimValueReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2SQLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.query.service.QueryService;
import com.tencent.supersonic.semantic.query.service.SchemaService;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class LocalSemanticInterpreter extends BaseSemanticInterpreter {

    private SchemaService schemaService;
    private DimensionService dimensionService;
    private MetricService metricService;
    private QueryService queryService;

    @SneakyThrows
    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructReq, User user) {
        if (StringUtils.isNotBlank(queryStructReq.getCorrectS2SQL())) {
            QueryS2SQLReq queryS2SQLReq = new QueryS2SQLReq();
            queryS2SQLReq.setSql(queryStructReq.getCorrectS2SQL());
            queryS2SQLReq.setModelId(queryStructReq.getModelId());
            queryS2SQLReq.setVariables(new HashMap<>());
            return queryByS2SQL(queryS2SQLReq, user);
        }
        queryService = ContextUtils.getBean(QueryService.class);
        return queryService.queryByStructWithAuth(queryStructReq, user);
    }

    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user) {
        try {
            queryService = ContextUtils.getBean(QueryService.class);
            return queryService.queryByMultiStruct(queryMultiStructReq, user);
        } catch (Exception e) {
            log.info("queryByMultiStruct has an exception:{}", e);
        }
        return null;
    }

    @Override
    @SneakyThrows
    public QueryResultWithSchemaResp queryByS2SQL(QueryS2SQLReq queryS2SQLReq, User user) {
        queryService = ContextUtils.getBean(QueryService.class);
        Object object = queryService.queryBySql(queryS2SQLReq, user);
        return JsonUtil.toObject(JsonUtil.toString(object), QueryResultWithSchemaResp.class);
    }

    @Override
    @SneakyThrows
    public QueryResultWithSchemaResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        queryService = ContextUtils.getBean(QueryService.class);
        return queryService.queryDimValue(queryDimValueReq, user);
    }

    @Override
    public List<ModelSchemaResp> doFetchModelSchema(List<Long> ids) {
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        filter.setModelIds(ids);
        schemaService = ContextUtils.getBean(SchemaService.class);
        User user = User.getFakeUser();
        return schemaService.fetchModelSchema(filter, user);
    }

    @Override
    public List<DomainResp> getDomainList(User user) {
        schemaService = ContextUtils.getBean(SchemaService.class);
        return schemaService.getDomainList(user);
    }

    @Override
    public List<ModelResp> getModelList(AuthType authType, Long domainId, User user) {
        schemaService = ContextUtils.getBean(SchemaService.class);
        return schemaService.getModelList(user, authType, domainId);
    }

    @Override
    public <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception {
        queryService = ContextUtils.getBean(QueryService.class);
        return queryService.explain(explainSqlReq, user);
    }

    @Override
    public PageInfo<DimensionResp> getDimensionPage(PageDimensionReq pageDimensionCmd) {
        dimensionService = ContextUtils.getBean(DimensionService.class);
        return dimensionService.queryDimension(pageDimensionCmd);
    }

    @Override
    public PageInfo<MetricResp> getMetricPage(PageMetricReq pageMetricReq, User user) {
        metricService = ContextUtils.getBean(MetricService.class);
        return metricService.queryMetric(pageMetricReq, user);
    }

}
