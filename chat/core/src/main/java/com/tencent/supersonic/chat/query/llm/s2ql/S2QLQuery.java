package com.tencent.supersonic.chat.query.llm.s2ql;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.parser.llm.s2ql.ParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.enums.QueryTypeEnum;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ExplainSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2QLReq;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class S2QLQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "LLM_S2QL";
    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    public S2QLQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        LLMResp llmResp = getLlmResp();

        long startTime = System.currentTimeMillis();
        QueryS2QLReq queryS2QLReq = getQueryS2QLReq(llmResp);
        QueryResultWithSchemaResp queryResp = semanticInterpreter.queryByS2QL(queryS2QLReq, user);

        log.info("queryByS2QL cost:{},querySql:{}", System.currentTimeMillis() - startTime, llmResp.getSqlOutput());

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

        parseInfo.setProperties(null);
        return queryResult;
    }

    private LLMResp getLlmResp() {
        String json = JsonUtil.toString(parseInfo.getProperties().get(Constants.CONTEXT));
        ParseResult parseResult = JsonUtil.toObject(json, ParseResult.class);
        return parseResult.getLlmResp();
    }

    private QueryS2QLReq getQueryS2QLReq(LLMResp llmResp) {
        return QueryReqBuilder.buildS2QLReq(llmResp.getCorrectorSql(), parseInfo.getModelId());
    }

    @Override
    public ExplainResp explain(User user) {
        ExplainSqlReq explainSqlReq = null;
        try {
            explainSqlReq = ExplainSqlReq.builder()
                    .queryTypeEnum(QueryTypeEnum.SQL)
                    .queryReq(getQueryS2QLReq(getLlmResp()))
                    .build();
            return semanticInterpreter.explain(explainSqlReq, user);
        } catch (Exception e) {
            log.error("explain error explainSqlReq:{}", explainSqlReq, e);
        }
        return null;
    }
}
