package com.tencent.supersonic.chat.query.dsl;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.parser.llm.DSLParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DSLQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "DSL";
    private DSLBuilder dslBuilder = new DSLBuilder();
    protected SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public DSLQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        String json = JsonUtil.toString(parseInfo.getProperties().get(Constants.CONTEXT));
        DSLParseResult dslParseResult = JsonUtil.toObject(json, DSLParseResult.class);
        LLMResp llmResp = dslParseResult.getLlmResp();
        QueryReq queryReq = dslParseResult.getRequest();

        Long modelId = parseInfo.getModelId();
        String querySql = convertToSql(queryReq.getQueryFilters(), llmResp, parseInfo, modelId);

        long startTime = System.currentTimeMillis();

        QueryDslReq queryDslReq = QueryReqBuilder.buildDslReq(querySql, modelId);
        QueryResultWithSchemaResp queryResp = semanticLayer.queryByDsl(queryDslReq, user);

        log.info("queryByDsl cost:{},querySql:{}", System.currentTimeMillis() - startTime, querySql);

        QueryResult queryResult = new QueryResult();
        if (Objects.nonNull(queryResp)) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }
        String resultQql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>() : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(resultQql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(QUERY_MODE);
        queryResult.setQueryState(QueryState.SUCCESS);

        // add model info
        EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class).getEntityInfo(parseInfo, user);
        queryResult.setEntityInfo(entityInfo);
        parseInfo.setProperties(null);
        return queryResult;
    }


    protected String convertToSql(QueryFilters queryFilters, LLMResp llmResp, SemanticParseInfo parseInfo,
            Long modelId) {
        try {
            return dslBuilder.build(parseInfo, queryFilters, llmResp, modelId);
        } catch (Exception e) {
            log.error("convertToSql error", e);
        }
        return null;
    }


}
