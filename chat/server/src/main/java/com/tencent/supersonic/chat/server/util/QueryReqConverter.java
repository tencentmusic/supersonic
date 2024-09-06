package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import org.apache.commons.collections.MapUtils;

import java.util.Objects;

public class QueryReqConverter {

    public static QueryNLReq buildText2SqlQueryReq(ParseContext parseContext) {
        return buildText2SqlQueryReq(parseContext, null);
    }

    public static QueryNLReq buildText2SqlQueryReq(ParseContext parseContext, ChatContext chatCtx) {
        QueryNLReq queryNLReq = new QueryNLReq();
        BeanMapper.mapper(parseContext, queryNLReq);
        Agent agent = parseContext.getAgent();
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
        if (chatCtx != null) {
            queryNLReq.setContextParseInfo(chatCtx.getParseInfo());
        }
        return queryNLReq;
    }
}
