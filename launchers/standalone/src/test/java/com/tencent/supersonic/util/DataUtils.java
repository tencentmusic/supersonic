package com.tencent.supersonic.util;

import static java.time.LocalDate.now;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.PluginTool;
import com.tencent.supersonic.chat.server.agent.RuleParserTool;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import java.util.HashSet;
import java.util.Set;

public class DataUtils {

    public static final Integer metricAgentId = 1;
    public static final Integer tagAgentId = 2;
    public static final Integer MULTI_TURNS_CHAT_ID = 11;
    private static final User user_test = User.getFakeUser();

    public static User getUser() {
        return user_test;
    }

    public static User getUserAlice() {
        return User.get(5L, "alice");
    }

    public static User getUserJack() {
        return User.get(2L, "jack");
    }

    public static User getUserTom() {
        return User.get(3L, "tom");
    }

    public static ChatParseReq getChatParseReq(Integer id, String query) {
        ChatParseReq chatParseReq = new ChatParseReq();
        chatParseReq.setQueryText(query);
        chatParseReq.setChatId(id);
        chatParseReq.setUser(user_test);
        return chatParseReq;
    }

    public static ChatParseReq getChatParseReqWithAgent(Integer id, String query, Integer agentId) {
        ChatParseReq queryReq = new ChatParseReq();
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

    public static DateConf getDateConf(DateConf.DateMode dateMode, String startDate, String endDate, int unit) {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(dateMode);
        dateInfo.setStartDate(startDate);
        dateInfo.setEndDate(endDate);
        dateInfo.setUnit(unit);
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
        ruleQueryTool.setDataSetIds(Lists.newArrayList(-1L));
        ruleQueryTool.setQueryModes(Lists.newArrayList("METRIC_ID", "METRIC_FILTER", "METRIC_MODEL",
                "TAG_DETAIL", "TAG_LIST_FILTER", "TAG_ID"));
        return ruleQueryTool;
    }

    private static PluginTool getPluginTool() {
        PluginTool pluginTool = new PluginTool();
        pluginTool.setType(AgentToolType.PLUGIN);
        pluginTool.setPlugins(Lists.newArrayList(1L));
        return pluginTool;
    }

    public static Set<Long> getMetricAgentIModelIds() {
        Set<Long> result = new HashSet<>();
        result.add(1L);
        result.add(2L);
        result.add(3L);
        return result;
    }

    public static Long getMetricAgentView() {
        return 1L;
    }
}
