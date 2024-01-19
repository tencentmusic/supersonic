package com.tencent.supersonic.chat.core.query.llm.analytics;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.knowledge.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.llm.LLMSemanticQuery;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.core.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.SemanticQueryResp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class MetricAnalyzeQuery extends LLMSemanticQuery {


    public static final String QUERY_MODE = "METRIC_INTERPRET";

    public MetricAnalyzeQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) throws SqlParseException {
        QueryStructReq queryStructReq = convertQueryStruct();
        SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        if (optimizationConfig.isUseS2SqlSwitch()) {
            queryStructReq.setS2SQL(parseInfo.getSqlInfo().getS2SQL());
            queryStructReq.setS2SQL(parseInfo.getSqlInfo().getQuerySQL());
        }

        SemanticQueryResp semanticQueryResp = semanticInterpreter.queryByStruct(queryStructReq, user);
        String text = generateTableText(semanticQueryResp);
        Map<String, Object> properties = parseInfo.getProperties();
        Map<String, String> replacedMap = new HashMap<>();
        String textReplaced = replaceText((String) properties.get("queryText"),
                parseInfo.getElementMatches(), replacedMap);
        String answer = replaceAnswer(fetchInterpret(textReplaced, text), replacedMap);
        QueryResult queryResult = new QueryResult();
        List<QueryColumn> queryColumns = Lists.newArrayList(new QueryColumn("结果", "string", "answer"));
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        List<Map<String, Object>> resultList = Lists.newArrayList();
        resultList.add(result);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(queryColumns);
        queryResult.setQueryMode(getQueryMode());
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

    @Override
    public void initS2Sql(SemanticSchema semanticSchema, User user) {
        initS2SqlByStruct(semanticSchema);
    }

    protected QueryStructReq convertQueryStruct() {
        QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(parseInfo);
        fillAggregator(queryStructReq, parseInfo.getMetrics());
        queryStructReq.setQueryType(QueryType.TAG);
        return queryStructReq;
    }

    private String replaceText(String text, List<SchemaElementMatch> schemaElementMatches,
            Map<String, String> replacedMap) {
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return text;
        }
        List<SchemaElementMatch> valueSchemaElementMatches = schemaElementMatches.stream()
                .filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                                || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : valueSchemaElementMatches) {
            String detectWord = schemaElementMatch.getDetectWord();
            if (StringUtils.isBlank(detectWord)) {
                continue;
            }
            text = text.replace(detectWord, "xxx");
            replacedMap.put("xxx", detectWord);
        }
        return text;
    }

    private void fillAggregator(QueryStructReq queryStructReq, Set<SchemaElement> schemaElements) {
        queryStructReq.getAggregators().clear();
        for (SchemaElement schemaElement : schemaElements) {
            Aggregator aggregator = new Aggregator();
            aggregator.setColumn(schemaElement.getBizName());
            aggregator.setFunc(AggOperatorEnum.SUM);
            aggregator.setNameCh(schemaElement.getName());
            queryStructReq.getAggregators().add(aggregator);
        }
    }

    private String replaceAnswer(String text, Map<String, String> replacedMap) {
        for (String key : replacedMap.keySet()) {
            text = text.replaceAll(key, replacedMap.get(key));
        }
        return text;
    }

    public static String generateTableText(SemanticQueryResp result) {
        StringBuilder tableBuilder = new StringBuilder();
        for (QueryColumn column : result.getColumns()) {
            tableBuilder.append(column.getName()).append("\t");
        }
        tableBuilder.append("\n");
        for (Map<String, Object> row : result.getResultList()) {
            for (QueryColumn column : result.getColumns()) {
                tableBuilder.append(row.get(column.getNameEn())).append("\t");
            }
            tableBuilder.append("\n");
        }
        return tableBuilder.toString();
    }

    public String fetchInterpret(String queryText, String dataText) {
        return "";
    }

}
