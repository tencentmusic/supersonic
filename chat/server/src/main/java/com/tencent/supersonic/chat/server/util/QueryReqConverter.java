package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import org.apache.commons.collections.MapUtils;

import java.util.Objects;

public class QueryReqConverter {

    public static QueryNLReq buildText2SqlQueryReq(ChatParseContext chatParseContext) {
        QueryNLReq queryNLReq = new QueryNLReq();
        BeanMapper.mapper(chatParseContext, queryNLReq);
        Agent agent = chatParseContext.getAgent();
        if (agent == null) {
            return queryNLReq;
        }

        boolean hasLLMTool = agent.containsLLMParserTool();
        boolean hasRuleTool = agent.containsRuleTool();
        boolean hasLLMConfig = Objects.nonNull(agent.getModelConfig());

        if (hasLLMTool && hasLLMConfig) {
            queryNLReq.setText2SQLType(Text2SQLType.ONLY_LLM);
        } else if (hasLLMTool && hasRuleTool) {
            queryNLReq.setText2SQLType(Text2SQLType.RULE_AND_LLM);
        } else if (hasLLMTool) {
            queryNLReq.setText2SQLType(Text2SQLType.ONLY_LLM);
        } else if (hasRuleTool) {
            queryNLReq.setText2SQLType(Text2SQLType.ONLY_RULE);
        }
        queryNLReq.setDataSetIds(agent.getDataSetIds());
        if (Objects.nonNull(queryNLReq.getMapInfo())
                && MapUtils.isNotEmpty(queryNLReq.getMapInfo().getDataSetElementMatches())) {
            queryNLReq.setMapInfo(queryNLReq.getMapInfo());
        }
        queryNLReq.setModelConfig(agent.getModelConfig());
        queryNLReq.setPromptConfig(agent.getPromptConfig());
        return queryNLReq;
    }

}
