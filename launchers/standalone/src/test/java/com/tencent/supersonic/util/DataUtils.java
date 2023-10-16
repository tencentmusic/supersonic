package com.tencent.supersonic.util;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.AgentConfig;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.MetricInterpretTool;
import com.tencent.supersonic.chat.agent.tool.PluginTool;
import com.tencent.supersonic.chat.agent.tool.RuleQueryTool;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.llm.interpret.MetricOption;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;

import java.util.Set;

import static java.time.LocalDate.now;

public class DataUtils {

    private static final User user_test = User.getFakeUser();

    public static User getUser() {
        return user_test;
    }

    public static QueryReq getQueryContextReq(Integer id, String query) {
        QueryReq queryContextReq = new QueryReq();
        queryContextReq.setQueryText(query);
        queryContextReq.setChatId(id);
        queryContextReq.setUser(user_test);
        return queryContextReq;
    }

    public static QueryReq getQueryReqWithAgent(Integer id, String query, Integer agentId) {
        QueryReq queryReq = new QueryReq();
        queryReq.setQueryText(query);
        queryReq.setChatId(id);
        queryReq.setUser(user_test);
        queryReq.setAgentId(agentId);
        return queryReq;
    }

    public static SchemaElement getSchemaElement(String name) {
        return SchemaElement.builder()
                .name(name)
                .build();
    }

    public static SchemaElement getMetric(Long modelId, Long id, String name, String bizName) {
        return SchemaElement.builder()
                .model(modelId)
                .id(id)
                .name(name)
                .bizName(bizName)
                .useCnt(0L)
                .type(SchemaElementType.METRIC)
                .build();
    }

    public static SchemaElement getDimension(Long modelId, Long id, String name, String bizName) {
        return SchemaElement.builder()
                .model(modelId)
                .id(id)
                .name(name)
                .bizName(bizName)
                .useCnt(null)
                .type(SchemaElementType.DIMENSION)
                .build();
    }

    public static QueryFilter getFilter(String bizName, FilterOperatorEnum filterOperatorEnum,
                                        Object value, String name, Long elementId) {
        QueryFilter filter = new QueryFilter();
        filter.setBizName(bizName);
        filter.setOperator(filterOperatorEnum);
        filter.setValue(value);
        filter.setName(name);
        filter.setElementID(elementId);
        return filter;
    }

    public static DateConf getDateConf(Integer unit, DateConf.DateMode dateMode, String period) {
        DateConf dateInfo = new DateConf();
        dateInfo.setUnit(unit);
        dateInfo.setDateMode(dateMode);
        dateInfo.setPeriod(period);
        dateInfo.setStartDate(now().plusDays(-unit).toString());
        dateInfo.setEndDate(now().plusDays(-1).toString());
        return dateInfo;
    }

    public static DateConf getDateConf(DateConf.DateMode dateMode, Integer unit,
                                       String period, String startDate, String endDate) {
        DateConf dateInfo = new DateConf();
        dateInfo.setUnit(unit);
        dateInfo.setDateMode(dateMode);
        dateInfo.setPeriod(period);
        dateInfo.setStartDate(startDate);
        dateInfo.setEndDate(endDate);
        return dateInfo;
    }

    public static DateConf getDateConf(DateConf.DateMode dateMode, String startDate, String endDate) {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(dateMode);
        dateInfo.setStartDate(startDate);
        dateInfo.setEndDate(endDate);
        return dateInfo;
    }

    public static Boolean compareDate(DateConf dateInfo1, DateConf dateInfo2) {
        Boolean timeFilterExist = dateInfo1.getUnit().equals(dateInfo2.getUnit())
                && dateInfo1.getDateMode().equals(dateInfo2.getDateMode())
                && dateInfo1.getPeriod().equals(dateInfo2.getPeriod());
        return timeFilterExist;
    }

    public static Boolean compareDateDimension(Set<SchemaElement> dimensions) {
        SchemaElement schemaItemDimension = new SchemaElement();
        schemaItemDimension.setBizName("sys_imp_date");
        Boolean dimensionExist = false;
        for (SchemaElement schemaItem : dimensions) {
            if (schemaItem.getBizName().equals(schemaItemDimension.getBizName())) {
                dimensionExist = true;
            }
        }
        return dimensionExist;
    }

    public static Boolean compareDimensionFilter(Set<QueryFilter> dimensionFilters, QueryFilter dimensionFilter) {
        Boolean dimensionFilterExist = false;
        for (QueryFilter filter : dimensionFilters) {
            if (filter.getBizName().equals(dimensionFilter.getBizName())
                    && filter.getOperator().equals(dimensionFilter.getOperator())
                    && filter.getValue().toString().equals(dimensionFilter.getValue().toString())
                    && filter.getElementID().equals(dimensionFilter.getElementID())
                    && filter.getName().equals(dimensionFilter.getName())) {
                dimensionFilterExist = true;
            }
        }
        return dimensionFilterExist;
    }


    public static Agent getAgent() {
        Agent agent = new Agent();
        agent.setId(1);
        agent.setName("查信息");
        agent.setDescription("查信息");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getTools().add(getRuleQueryTool());
        agentConfig.getTools().add(getPluginTool());
        agentConfig.getTools().add(getMetricInterpretTool());
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        return agent;
    }

    private static RuleQueryTool getRuleQueryTool() {
        RuleQueryTool ruleQueryTool = new RuleQueryTool();
        ruleQueryTool.setType(AgentToolType.RULE);
        ruleQueryTool.setModelIds(Lists.newArrayList(1L, 2L));
        ruleQueryTool.setQueryModes(Lists.newArrayList("METRIC_ENTITY", "METRIC_FILTER", "METRIC_MODEL"));
        return ruleQueryTool;
    }

    private static PluginTool getPluginTool() {
        PluginTool pluginTool = new PluginTool();
        pluginTool.setType(AgentToolType.PLUGIN);
        pluginTool.setPlugins(Lists.newArrayList(1L));
        return pluginTool;
    }

    private static MetricInterpretTool getMetricInterpretTool() {
        MetricInterpretTool metricInterpretTool = new MetricInterpretTool();
        metricInterpretTool.setModelId(1L);
        metricInterpretTool.setType(AgentToolType.INTERPRET);
        metricInterpretTool.setMetricOptions(Lists.newArrayList(
                new MetricOption(1L),
                new MetricOption(2L),
                new MetricOption(3L)));
        return metricInterpretTool;
    }

}
