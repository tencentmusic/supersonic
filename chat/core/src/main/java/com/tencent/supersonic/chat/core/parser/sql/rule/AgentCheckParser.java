package com.tencent.supersonic.chat.core.parser.sql.rule;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.core.agent.AgentToolType;
import com.tencent.supersonic.chat.core.agent.RuleParserTool;
import com.tencent.supersonic.chat.core.parser.SemanticParser;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class AgentCheckParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> queries = queryContext.getCandidateQueries();
        agentCanSupport(queryContext, queries);
    }

    private void agentCanSupport(QueryContext queryContext, List<SemanticQuery> queries) {
        Agent agent = queryContext.getAgent();
        if (agent == null) {
            return;
        }
        List<RuleParserTool> queryTools = getRuleTools(agent);
        if (CollectionUtils.isEmpty(queryTools)) {
            queries.clear();
            return;
        }
        log.info("queries resolved:{} {}", agent.getName(),
                queries.stream().map(SemanticQuery::getQueryMode).collect(Collectors.toList()));
        queries.removeIf(query -> {
            for (RuleParserTool tool : queryTools) {
                if (CollectionUtils.isNotEmpty(tool.getQueryModes())
                        && !tool.getQueryModes().contains(query.getQueryMode())) {
                    return true;
                }
                if (CollectionUtils.isNotEmpty(tool.getQueryTypes())) {
                    if (QueryManager.isTagQuery(query.getQueryMode())) {
                        return !tool.getQueryTypes().contains(QueryType.TAG.name());
                    }
                    if (QueryManager.isMetricQuery(query.getQueryMode())) {
                        return !tool.getQueryTypes().contains(QueryType.METRIC.name());
                    }
                }
                if (CollectionUtils.isEmpty(tool.getModelIds())) {
                    return true;
                }
                if (tool.isContainsAllModel()) {
                    return false;
                }
                if (new HashSet<>(tool.getModelIds())
                        .containsAll(query.getParseInfo().getModel().getModelIds())) {
                    return false;
                }
            }
            return true;
        });
        log.info("rule queries witch can be supported by agent :{} {}", agent.getName(),
                queries.stream().map(SemanticQuery::getQueryMode).collect(Collectors.toList()));
    }

    private static List<RuleParserTool> getRuleTools(Agent agent) {
        if (agent == null) {
            return Lists.newArrayList();
        }
        List<String> tools = agent.getTools(AgentToolType.NL2SQL_RULE);
        if (CollectionUtils.isEmpty(tools)) {
            return Lists.newArrayList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, RuleParserTool.class))
                .collect(Collectors.toList());
    }

}
