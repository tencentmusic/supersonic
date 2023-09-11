package com.tencent.supersonic.chat.parser.rule;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.RuleQueryTool;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AgentCheckParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> queries = queryContext.getCandidateQueries();
        agentCanSupport(queryContext.getRequest().getAgentId(), queries);
    }

    private void agentCanSupport(Integer agentId, List<SemanticQuery> queries) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return;
        }
        List<RuleQueryTool> queryTools = getRuleTools(agentId);
        if (CollectionUtils.isEmpty(queryTools)) {
            queries.clear();
            return;
        }
        log.info("queries resolved:{} {}", agent.getName(),
                queries.stream().map(SemanticQuery::getQueryMode).collect(Collectors.toList()));
        queries.removeIf(query -> {
            for (RuleQueryTool tool : queryTools) {
                if (!tool.getQueryModes().contains(query.getQueryMode())) {
                    return true;
                }
                if (CollectionUtils.isEmpty(tool.getModelIds())) {
                    return true;
                }
                if (tool.isContainsAllModel() || tool.getModelIds().contains(query.getParseInfo().getModelId())) {
                    return false;
                }
            }
            return true;
        });
        log.info("rule queries witch can be supported by agent :{} {}", agent.getName(),
                queries.stream().map(SemanticQuery::getQueryMode).collect(Collectors.toList()));
    }

    private static List<RuleQueryTool> getRuleTools(Integer agentId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return Lists.newArrayList();
        }
        List<String> tools = agent.getTools(AgentToolType.RULE);
        if (CollectionUtils.isEmpty(tools)) {
            return Lists.newArrayList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, RuleQueryTool.class))
                .collect(Collectors.toList());
    }

}
