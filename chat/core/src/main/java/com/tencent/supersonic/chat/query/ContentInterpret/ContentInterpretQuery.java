package com.tencent.supersonic.chat.query.ContentInterpret;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class ContentInterpretQuery extends PluginSemanticQuery {

    @Override
    public String getQueryMode() {
        return "CONTENT_INTERPRET";
    }

    public ContentInterpretQuery() {
        QueryManager.register(this);
    }

    @Override
    public QueryResult execute(User user) throws SqlParseException {
        QueryResultWithSchemaResp queryResultWithSchemaResp = queryMetric(user);
        String text = generateDataText(queryResultWithSchemaResp);
        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(JsonUtil.toString(properties.get(Constants.CONTEXT))
                , PluginParseResult.class);
        String answer = fetchInterpret(pluginParseResult.getRequest().getQueryText(), text);
        QueryResult queryResult = new QueryResult();
        List<QueryColumn> queryColumns = Lists.newArrayList(new QueryColumn("结果", "string", "answer"));
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        List<Map<String, Object>> resultList = Lists.newArrayList();
        resultList.add(result);
        queryResultWithSchemaResp.setResultList(resultList);
        queryResultWithSchemaResp.setColumns(queryColumns);
        queryResult.setResponse(queryResultWithSchemaResp);
        queryResult.setQueryMode(getQueryMode());
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }


    private QueryResultWithSchemaResp queryMetric(User user) {
        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setModelId(parseInfo.getModelId());
        queryStructReq.setGroups(Lists.newArrayList(TimeDimensionEnum.DAY.getName()));
        ModelSchema modelSchema = semanticLayer.getModelSchema(parseInfo.getModelId(), true);
        queryStructReq.setAggregators(buildAggregator(modelSchema));
        List<Filter> filterList = Lists.newArrayList();
        for (QueryFilter queryFilter : parseInfo.getDimensionFilters()) {
            Filter filter = new Filter();
            BeanUtils.copyProperties(queryFilter, filter);
            filterList.add(filter);
        }
        queryStructReq.setDimensionFilters(filterList);
        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.RECENT);
        dateConf.setUnit(7);
        queryStructReq.setDateInfo(dateConf);
        return semanticLayer.queryByStruct(queryStructReq, user);
    }

    private List<Aggregator> buildAggregator(ModelSchema modelSchema) {
        List<Aggregator> aggregators = Lists.newArrayList();
        Set<SchemaElement> metrics = modelSchema.getMetrics();
        if (CollectionUtils.isEmpty(metrics)) {
            return aggregators;
        }
        for (SchemaElement schemaElement : metrics) {
            Aggregator aggregator = new Aggregator();
            aggregator.setColumn(schemaElement.getBizName());
            aggregator.setFunc(AggOperatorEnum.SUM);
            aggregator.setNameCh(schemaElement.getName());
            aggregators.add(aggregator);
        }
        return aggregators;
    }


    public String generateDataText(QueryResultWithSchemaResp queryResultWithSchemaResp) {
        Map<String, String> map = queryResultWithSchemaResp.getColumns().stream()
                .collect(Collectors.toMap(QueryColumn::getNameEn, QueryColumn::getName));
        StringBuilder stringBuilder = new StringBuilder();
        for (Map<String, Object> valueMap : queryResultWithSchemaResp.getResultList()) {
            for (String key : valueMap.keySet()) {
                String name = "";
                if (TimeDimensionEnum.getNameList().contains(key)) {
                    name = "日期";
                } else {
                    name = map.get(key);
                }
                String value = String.valueOf(valueMap.get(key));
                stringBuilder.append(name).append(":").append(value).append(" ");
            }
        }
        return stringBuilder.toString();
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
            return lLmAnswerResp.getAssistant_message();
        }
        return null;
    }


}