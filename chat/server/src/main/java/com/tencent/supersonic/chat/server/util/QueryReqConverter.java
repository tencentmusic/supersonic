package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryTextReq;
import org.apache.commons.collections.MapUtils;

import java.util.Objects;

public class QueryReqConverter {

    public static QueryTextReq buildText2SqlQueryReq(ChatParseContext chatParseContext) {
        QueryTextReq queryTextReq = new QueryTextReq();
        BeanMapper.mapper(chatParseContext, queryTextReq);
        Agent agent = chatParseContext.getAgent();
        if (agent == null) {
            return queryTextReq;
        }
        if (agent.containsLLMParserTool() && agent.containsRuleTool()) {
            queryTextReq.setText2SQLType(Text2SQLType.RULE_AND_LLM);
        } else if (agent.containsLLMParserTool()) {
            queryTextReq.setText2SQLType(Text2SQLType.ONLY_LLM);
        } else if (agent.containsRuleTool()) {
            queryTextReq.setText2SQLType(Text2SQLType.ONLY_RULE);
        }
        queryTextReq.setDataSetIds(agent.getDataSetIds());
        if (Objects.nonNull(queryTextReq.getMapInfo())
                && MapUtils.isNotEmpty(queryTextReq.getMapInfo().getDataSetElementMatches())) {
            queryTextReq.setMapInfo(queryTextReq.getMapInfo());
        }
        queryTextReq.setLlmConfig(agent.getLlmConfig());
        queryTextReq.setPromptConfig(agent.getPromptConfig());
        return queryTextReq;
    }

}
