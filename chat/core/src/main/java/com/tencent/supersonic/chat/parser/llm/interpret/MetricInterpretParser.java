package com.tencent.supersonic.chat.parser.llm.interpret;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.MetricInterpretTool;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.query.llm.interpret.MetricInterpretQuery;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
public class MetricInterpretParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        if (SatisfactionChecker.check(queryContext)) {
            log.info("skip MetricInterpretParser");
            return;
        }
        Map<Long, MetricInterpretTool> metricInterpretToolMap =
                getMetricInterpretTools(queryContext.getRequest().getAgentId());
        log.info("metric interpret tool : {}", metricInterpretToolMap);
        if (CollectionUtils.isEmpty(metricInterpretToolMap)) {
            return;
        }
        Map<Long, List<SchemaElementMatch>> elementMatches = queryContext.getMapInfo().getModelElementMatches();
        for (Long modelId : elementMatches.keySet()) {
            MetricInterpretTool metricInterpretTool = metricInterpretToolMap.get(modelId);
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
        PluginSemanticQuery metricInterpretQuery = QueryManager.createPluginQuery(MetricInterpretQuery.QUERY_MODE);
        Set<SchemaElement> metrics = getMetrics(metricIds, modelId);
        SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(modelId, queryContext.getRequest(),
                metrics, schemaElementMatches, toolName);
        semanticParseInfo.setQueryMode(metricInterpretQuery.getQueryMode());
        semanticParseInfo.getProperties().put("queryText", queryContext.getRequest().getQueryText());
        metricInterpretQuery.setParseInfo(semanticParseInfo);
        queryContext.getCandidateQueries().add(metricInterpretQuery);
    }

    public Set<SchemaElement> getMetrics(List<Long> metricIds, Long modelId) {
        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
        ModelSchema modelSchema = semanticLayer.getModelSchema(modelId, true);
        Set<SchemaElement> metrics = modelSchema.getMetrics();
        return metrics.stream().filter(schemaElement -> metricIds.contains(schemaElement.getId()))
                .collect(Collectors.toSet());
    }

    private Map<Long, MetricInterpretTool> getMetricInterpretTools(Integer agentId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return new HashMap<>();
        }
        List<String> tools = agent.getTools(AgentToolType.INTERPRET);
        if (CollectionUtils.isEmpty(tools)) {
            return new HashMap<>();
        }
        List<MetricInterpretTool> metricInterpretTools = tools.stream().map(tool ->
                        JSONObject.parseObject(tool, MetricInterpretTool.class))
                .filter(tool -> !CollectionUtils.isEmpty(tool.getMetricOptions()))
                .collect(Collectors.toList());
        Map<Long, MetricInterpretTool> metricInterpretToolMap = new HashMap<>();
        for (MetricInterpretTool metricInterpretTool : metricInterpretTools) {
            metricInterpretToolMap.putIfAbsent(metricInterpretTool.getModelId(),
                    metricInterpretTool);
        }
        return metricInterpretToolMap;
    }

    private SemanticParseInfo buildSemanticParseInfo(Long modelId, QueryReq queryReq, Set<SchemaElement> metrics,
                                                     List<SchemaElementMatch> schemaElementMatches, String toolName) {
        SchemaElement model = new SchemaElement();
        model.setModel(modelId);
        model.setId(modelId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setMetrics(metrics);
        SchemaElement dimension = new SchemaElement();
        dimension.setBizName(TimeDimensionEnum.DAY.getName());
        semanticParseInfo.setDimensions(Sets.newHashSet(dimension));
        semanticParseInfo.setElementMatches(schemaElementMatches);
        semanticParseInfo.setModel(model);
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
