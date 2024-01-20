package com.tencent.supersonic.util;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.core.agent.AgentConfig;
import com.tencent.supersonic.chat.core.agent.AgentToolType;
import com.tencent.supersonic.chat.core.agent.PluginTool;
import com.tencent.supersonic.chat.core.agent.RuleParserTool;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;

import static java.time.LocalDate.now;

public class DataUtils {

    public static final Integer metricAgentId = 1;
    public static final Integer tagAgentId = 2;
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

    public static Agent getMetricAgent() {
        Agent agent = new Agent();
        agent.setId(1);
        agent.setName("查信息");
        agent.setDescription("查信息");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getTools().add(getRuleQueryTool());
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        return agent;
    }

    public static Agent getTagAgent() {
        Agent agent = new Agent();
        agent.setId(2);
        agent.setName("标签圈选");
        agent.setDescription("标签圈选");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getTools().add(getRuleQueryTool());
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        return agent;
    }

    private static RuleParserTool getRuleQueryTool() {
        RuleParserTool ruleQueryTool = new RuleParserTool();
        ruleQueryTool.setType(AgentToolType.NL2SQL_RULE);
        ruleQueryTool.setModelIds(Lists.newArrayList(-1L));
        ruleQueryTool.setQueryModes(Lists.newArrayList("METRIC_TAG", "METRIC_FILTER", "METRIC_MODEL",
                "TAG_DETAIL", "TAG_LIST_FILTER", "TAG_ID"));
        return ruleQueryTool;
    }

    private static PluginTool getPluginTool() {
        PluginTool pluginTool = new PluginTool();
        pluginTool.setType(AgentToolType.PLUGIN);
        pluginTool.setPlugins(Lists.newArrayList(1L));
        return pluginTool;
    }

}
