package com.tencent.supersonic.chat.query.llm.interpret;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MetricInterpretQuery extends PluginSemanticQuery {


    public static final String QUERY_MODE = "METRIC_INTERPRET";

    public MetricInterpretQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) throws SqlParseException {
        QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(parseInfo);
        fillAggregator(queryStructReq, parseInfo.getMetrics());
        queryStructReq.setNativeQuery(true);
        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
        QueryResultWithSchemaResp queryResultWithSchemaResp = semanticLayer.queryByStruct(queryStructReq, user);
        String text = generateTableText(queryResultWithSchemaResp);
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

    public static String generateTableText(QueryResultWithSchemaResp result) {
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
        PluginManager pluginManager = ContextUtils.getBean(PluginManager.class);
        LLmAnswerReq lLmAnswerReq = new LLmAnswerReq();
        lLmAnswerReq.setQueryText(queryText);
        lLmAnswerReq.setPluginOutput(dataText);
        ResponseEntity<String> responseEntity = pluginManager.doRequest("answer_with_plugin_call",
                JSONObject.toJSONString(lLmAnswerReq));
        LLmAnswerResp lLmAnswerResp = JSONObject.parseObject(responseEntity.getBody(), LLmAnswerResp.class);
        if (lLmAnswerResp != null) {
            return lLmAnswerResp.getAssistantMessage();
        }
        return null;
    }


}
