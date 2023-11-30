package com.tencent.supersonic.chat.parser.analytics;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.AgentToolType;
import com.tencent.supersonic.chat.agent.DataAnalyticsTool;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.LLMSemanticQuery;
import com.tencent.supersonic.chat.query.llm.analytics.MetricAnalyzeQuery;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ModelCluster;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class MetricAnalyzeParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        if (SatisfactionChecker.isSkip(queryContext)) {
            log.info("skip MetricAnalyzeParser");
            return;
        }
        Map<Long, DataAnalyticsTool> metricInterpretToolMap =
                getMetricInterpretTools(queryContext.getRequest().getAgentId());
        log.info("metric interpret tool : {}", metricInterpretToolMap);
        if (CollectionUtils.isEmpty(metricInterpretToolMap)) {
            return;
        }
        Map<Long, List<SchemaElementMatch>> elementMatches = queryContext.getMapInfo().getModelElementMatches();
        for (Long modelId : elementMatches.keySet()) {
            DataAnalyticsTool metricInterpretTool = metricInterpretToolMap.get(modelId);
            if (metricInterpretTool == null) {
                continue;
            }
            if (CollectionUtils.isEmpty(elementMatches.get(modelId))) {
                continue;
            }
            List<MetricOption> metricOptions = metricInterpretTool.getMetricOptions();
            if (!CollectionUtils.isEmpty(metricOptions)) {
                List<Long> metricIds = metricOptions.stream()
                        .map(MetricOption::getMetricId).collect(Collectors.toList());
                String name = metricInterpretTool.getName();
                buildQuery(modelId, queryContext, metricIds, elementMatches.get(modelId), name);
            }
        }
    }

    private void buildQuery(Long modelId, QueryContext queryContext,
                            List<Long> metricIds, List<SchemaElementMatch> schemaElementMatches, String toolName) {
        LLMSemanticQuery metricInterpretQuery = QueryManager.createLLMQuery(MetricAnalyzeQuery.QUERY_MODE);
        Set<SchemaElement> metrics = getMetrics(metricIds, modelId);
        SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(modelId, queryContext.getRequest(),
                metrics, schemaElementMatches, toolName);
        semanticParseInfo.setQueryMode(metricInterpretQuery.getQueryMode());
        semanticParseInfo.getProperties().put("queryText", queryContext.getRequest().getQueryText());
        metricInterpretQuery.setParseInfo(semanticParseInfo);
        queryContext.getCandidateQueries().add(metricInterpretQuery);
    }

    public Set<SchemaElement> getMetrics(List<Long> metricIds, Long modelId) {
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        List<SchemaElement> metrics = semanticService.getSemanticSchema().getMetrics();
        return metrics.stream().filter(schemaElement -> metricIds.contains(schemaElement.getId()))
                .collect(Collectors.toSet());
    }

    private Map<Long, DataAnalyticsTool> getMetricInterpretTools(Integer agentId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return new HashMap<>();
        }
        List<String> tools = agent.getTools(AgentToolType.ANALYTICS);
        if (CollectionUtils.isEmpty(tools)) {
            return new HashMap<>();
        }
        List<DataAnalyticsTool> metricInterpretTools = tools.stream().map(tool ->
                        JSONObject.parseObject(tool, DataAnalyticsTool.class))
                .filter(tool -> !CollectionUtils.isEmpty(tool.getMetricOptions()))
                .collect(Collectors.toList());
        Map<Long, DataAnalyticsTool> metricInterpretToolMap = new HashMap<>();
        for (DataAnalyticsTool metricInterpretTool : metricInterpretTools) {
            metricInterpretToolMap.putIfAbsent(metricInterpretTool.getModelId(),
                    metricInterpretTool);
        }
        return metricInterpretToolMap;
    }

    private SemanticParseInfo buildSemanticParseInfo(Long modelId, QueryReq queryReq, Set<SchemaElement> metrics,
                                                     List<SchemaElementMatch> schemaElementMatches, String toolName) {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setMetrics(metrics);
        SchemaElement dimension = new SchemaElement();
        dimension.setBizName(TimeDimensionEnum.DAY.getName());
        semanticParseInfo.setDimensions(Sets.newHashSet(dimension));
        semanticParseInfo.setElementMatches(schemaElementMatches);
        semanticParseInfo.setModel(ModelCluster.build(Sets.newHashSet(modelId)));
        semanticParseInfo.setScore(queryReq.getQueryText().length());
        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.RECENT);
        dateConf.setUnit(15);
        semanticParseInfo.setDateInfo(dateConf);
        Map<String, Object> properties = new HashMap<>();
        properties.put("type", "internal");
        properties.put("name", toolName);
        semanticParseInfo.setProperties(properties);
        fillSemanticParseInfo(semanticParseInfo);
        return semanticParseInfo;
    }

    private void fillSemanticParseInfo(SemanticParseInfo semanticParseInfo) {
        List<SchemaElementMatch> schemaElementMatches = semanticParseInfo.getElementMatches();
        if (!CollectionUtils.isEmpty(schemaElementMatches)) {
            schemaElementMatches.stream().filter(schemaElementMatch ->
                            SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                                    || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                    .forEach(schemaElementMatch -> {
                        QueryFilter queryFilter = new QueryFilter();
                        queryFilter.setValue(schemaElementMatch.getWord());
                        queryFilter.setElementID(schemaElementMatch.getElement().getId());
                        queryFilter.setName(schemaElementMatch.getElement().getName());
                        queryFilter.setOperator(FilterOperatorEnum.EQUALS);
                        queryFilter.setBizName(schemaElementMatch.getElement().getBizName());
                        semanticParseInfo.getDimensionFilters().add(queryFilter);
                    });
        }
    }

}
