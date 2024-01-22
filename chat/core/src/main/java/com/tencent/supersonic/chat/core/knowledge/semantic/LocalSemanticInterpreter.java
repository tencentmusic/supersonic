package com.tencent.supersonic.chat.core.knowledge.semantic;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import java.util.ArrayList;
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
    public SemanticQueryResp queryByStruct(QueryStructReq queryStructReq, User user) {
        if (StringUtils.isNotBlank(queryStructReq.getCorrectS2SQL())) {
            QuerySqlReq querySQLReq = new QuerySqlReq();
            querySQLReq.setSql(queryStructReq.getCorrectS2SQL());
            querySQLReq.setModelIds(queryStructReq.getModelIdSet());
            querySQLReq.setParams(new ArrayList<>());
            return queryByS2SQL(querySQLReq, user);
        }
        queryService = ContextUtils.getBean(QueryService.class);
        return queryService.queryByStructWithAuth(queryStructReq, user);
    }

    @Override
    public SemanticQueryResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user) {
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
    public SemanticQueryResp queryByS2SQL(QuerySqlReq querySQLReq, User user) {
        queryService = ContextUtils.getBean(QueryService.class);
        SemanticQueryResp object = queryService.queryBySql(querySQLReq, user);
        return JsonUtil.toObject(JsonUtil.toString(object), SemanticQueryResp.class);
    }

    @Override
    @SneakyThrows
    public SemanticQueryResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
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
