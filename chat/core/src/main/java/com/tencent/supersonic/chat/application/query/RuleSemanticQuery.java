
package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.application.DomainEntityService;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

@Slf4j
@ToString
public abstract class RuleSemanticQuery implements SemanticQuery, Serializable {

    protected SemanticParseInfo parseInfo = new SemanticParseInfo();
    protected QueryMatcher queryMatcher = new QueryMatcher();
    protected SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public RuleSemanticQuery() {
        RuleSemanticQueryManager.register(this);
    }

    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
                                   QueryContextReq queryCtx) {
        return queryMatcher.match(candidateElementMatches);
    }

    public abstract void inheritContext(ChatContext chatContext);

    @Override
    public QueryResultResp execute(User user) {
        String queryMode = parseInfo.getQueryMode();

        if (parseInfo.getDomainId() < 0 || StringUtils.isEmpty(queryMode)) {
            // reach here some error may happen
            log.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        List<String> semanticQueryModes = RuleSemanticQueryManager.getSemanticQueryModes();
        if (!semanticQueryModes.contains(parseInfo.getQueryMode())) {
            return null;
        }

        QueryResultResp queryResponse = new QueryResultResp();
        QueryResultWithSchemaResp queryResult = semanticLayer.queryByStruct(
                SchemaInfoConverter.convertTo(parseInfo), user);


        if (queryResult != null) {
            queryResponse.setQueryAuthorization(queryResult.getQueryAuthorization());
        }
        String sql = queryResult == null ? null : queryResult.getSql();
        List<Map<String, Object>> resultList = queryResult == null ? new ArrayList<>()
                : queryResult.getResultList();
        List<QueryColumn> columns = queryResult == null ? new ArrayList<>() : queryResult.getColumns();
        queryResponse.setQuerySql(sql);
        queryResponse.setQueryResults(resultList);
        queryResponse.setQueryColumns(columns);
        queryResponse.setQueryMode(queryMode);

        // add domain info
        EntityInfo entityInfo = ContextUtils.getBean(DomainEntityService.class)
                .getEntityInfo(parseInfo, user);
        queryResponse.setEntityInfo(entityInfo);
        return queryResponse;
    }

    @Override
    public SemanticParseInfo getParseInfo() {
        return parseInfo;
    }

    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }
}