
package com.tencent.supersonic.chat.query.rule;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.io.Serializable;
import java.util.*;

@Slf4j
@ToString
public abstract class RuleSemanticQuery implements SemanticQuery, Serializable {

    protected SemanticParseInfo parseInfo = new SemanticParseInfo();
    protected QueryMatcher queryMatcher = new QueryMatcher();
    protected SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public RuleSemanticQuery() {
        QueryManager.register(this);
    }

    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
                                          QueryContext queryCtx) {
        return queryMatcher.match(candidateElementMatches);
    }

    public void fillParseInfo(Long domainId, ChatContext chatContext) {
        parseInfo.setQueryMode(getQueryMode());

        SemanticService schemaService = ContextUtils.getBean(SemanticService.class);
        DomainSchema domainSchema = schemaService.getDomainSchema(domainId);

        fillSchemaElement(parseInfo, domainSchema);
        // inherit date info from context
        if (parseInfo.getDateInfo() == null && chatContext.getParseInfo().getDateInfo() != null
                && isSameQueryMode(getQueryMode(), chatContext.getParseInfo().getQueryMode())) {
            log.info("inherit date info from context");
            parseInfo.setDateInfo(chatContext.getParseInfo().getDateInfo());
        }
    }

    public boolean isSameQueryMode(String queryModeQuery, String queryModeChat) {
        if (Strings.isNotEmpty(queryModeQuery) && Strings.isNotEmpty(queryModeChat)) {
            return QueryManager.isEntityQuery(queryModeQuery) && QueryManager.isEntityQuery(queryModeChat)
                    || QueryManager.isMetricQuery(queryModeQuery) && QueryManager.isMetricQuery(queryModeChat);
        }
        return true;
    }

    private void fillSchemaElement(SemanticParseInfo parseInfo, DomainSchema domainSchema) {
        parseInfo.setDomain(domainSchema.getDomain());

        Map<Long, List<SchemaElementMatch>> dim2Values = new HashMap<>();
        Map<Long, List<SchemaElementMatch>> id2Values = new HashMap<>();

        for (SchemaElementMatch schemaMatch : parseInfo.getElementMatches()) {
            SchemaElement element = schemaMatch.getElement();
            switch (element.getType()) {
                case ID:
                    SchemaElement entityElement = domainSchema.getElement(SchemaElementType.ENTITY, element.getId());
                    if (entityElement != null) {
                        if (id2Values.containsKey(element.getId())) {
                            id2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            id2Values.put(element.getId(), new ArrayList<>(Arrays.asList(schemaMatch)));
                        }
                    }
                    break;
                case VALUE:
                    SchemaElement dimElement = domainSchema.getElement(SchemaElementType.DIMENSION, element.getId());
                    if (dimElement != null) {
                        if (dim2Values.containsKey(element.getId())) {
                            dim2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            dim2Values.put(element.getId(), new ArrayList<>(Arrays.asList(schemaMatch)));
                        }
                    }
                    break;
                case DIMENSION:
                    parseInfo.getDimensions().add(element);
                    break;
                case METRIC:
                    parseInfo.getMetrics().add(element);
                    break;
                case ENTITY:
                    parseInfo.setEntity(element);
                    break;
                default:
            }
        }

        if (!id2Values.isEmpty()) {
            for (Map.Entry<Long, List<SchemaElementMatch>> entry : id2Values.entrySet()) {
                SchemaElement entity = domainSchema.getElement(SchemaElementType.ENTITY, entry.getKey());

                if (entry.getValue().size() == 1) {
                    SchemaElementMatch schemaMatch = entry.getValue().get(0);
                    QueryFilter dimensionFilter = new QueryFilter();
                    dimensionFilter.setValue(schemaMatch.getWord());
                    dimensionFilter.setBizName(entity.getBizName());
                    dimensionFilter.setName(entity.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
                    dimensionFilter.setElementID(schemaMatch.getElement().getId());
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                    parseInfo.setEntity(domainSchema.getEntity());
                } else {
                    QueryFilter dimensionFilter = new QueryFilter();
                    List<String> vals = new ArrayList<>();
                    entry.getValue().stream().forEach(i -> vals.add(i.getWord()));
                    dimensionFilter.setValue(vals);
                    dimensionFilter.setBizName(entity.getBizName());
                    dimensionFilter.setName(entity.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.IN);
                    dimensionFilter.setElementID(entry.getKey());
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                }
            }
        }

        if (!dim2Values.isEmpty()) {
            for (Map.Entry<Long, List<SchemaElementMatch>> entry : dim2Values.entrySet()) {
                SchemaElement dimension = domainSchema.getElement(SchemaElementType.DIMENSION, entry.getKey());

                if (entry.getValue().size() == 1) {
                    SchemaElementMatch schemaMatch = entry.getValue().get(0);
                    QueryFilter dimensionFilter = new QueryFilter();
                    dimensionFilter.setValue(schemaMatch.getWord());
                    dimensionFilter.setBizName(dimension.getBizName());
                    dimensionFilter.setName(dimension.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
                    dimensionFilter.setElementID(schemaMatch.getElement().getId());
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                    parseInfo.setEntity(domainSchema.getEntity());
                } else {
                    QueryFilter dimensionFilter = new QueryFilter();
                    List<String> vals = new ArrayList<>();
                    entry.getValue().stream().forEach(i -> vals.add(i.getWord()));
                    dimensionFilter.setValue(vals);
                    dimensionFilter.setBizName(dimension.getBizName());
                    dimensionFilter.setName(dimension.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.IN);
                    dimensionFilter.setElementID(entry.getKey());
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                }
            }
        }
    }


    @Override
    public QueryResult execute(User user) {
        String queryMode = parseInfo.getQueryMode();

        if (parseInfo.getDomainId() < 0 || StringUtils.isEmpty(queryMode)
                || !QueryManager.containsRuleQuery(queryMode)) {
            // reach here some error may happen
            log.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        QueryResult queryResult = new QueryResult();
        QueryResultWithSchemaResp queryResp = semanticLayer.queryByStruct(
                convertQueryStruct(), user);

        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }
        String sql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>()
                : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(sql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(queryMode);
        queryResult.setQueryState(QueryState.SUCCESS);

        // add domain info
        EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class)
                .getEntityInfo(parseInfo, user);
        queryResult.setEntityInfo(entityInfo);
        return queryResult;
    }

    public QueryResult multiStructExecute(User user) {
        String queryMode = parseInfo.getQueryMode();

        if (parseInfo.getDomainId() < 0 || StringUtils.isEmpty(queryMode)
                || !QueryManager.containsRuleQuery(queryMode)) {
            // reach here some error may happen
            log.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        QueryResult queryResult = new QueryResult();
        QueryMultiStructReq queryMultiStructReq = convertQueryMultiStruct();
        QueryResultWithSchemaResp queryResp = semanticLayer.queryByMultiStruct(queryMultiStructReq, user);
        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }
        String sql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>()
                : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(sql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(queryMode);
        queryResult.setQueryState(QueryState.SUCCESS);

        // add domain info
        EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class)
                .getEntityInfo(parseInfo, user);
        queryResult.setEntityInfo(entityInfo);
        return queryResult;
    }

    @Override
    public SemanticParseInfo getParseInfo() {
        return parseInfo;
    }

    @Override
    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }

    public static List<RuleSemanticQuery> resolve(List<SchemaElementMatch> candidateElementMatches,
                                                  QueryContext queryContext) {
        List<RuleSemanticQuery> matchedQueries = new ArrayList<>();
        for (RuleSemanticQuery semanticQuery : QueryManager.getRuleQueries()) {
            List<SchemaElementMatch> matches = semanticQuery.match(candidateElementMatches, queryContext);

            if (matches.size() > 0) {
                RuleSemanticQuery query = QueryManager.createRuleQuery(semanticQuery.getQueryMode());
                query.getParseInfo().getElementMatches().addAll(matches);
                matchedQueries.add(query);
            }
        }

        return matchedQueries;
    }

    protected QueryStructReq convertQueryStruct() {
        return QueryReqBuilder.buildStructReq(parseInfo);
    }

    protected QueryMultiStructReq convertQueryMultiStruct() {
        return QueryReqBuilder.buildMultiStructReq(parseInfo);
    }

}
