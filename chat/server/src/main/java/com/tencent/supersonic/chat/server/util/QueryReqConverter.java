package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;

public class QueryReqConverter {

    public static QueryReq buildText2SqlQueryReq(ChatParseContext chatParseContext) {
        QueryReq queryReq = new QueryReq();
        BeanMapper.mapper(chatParseContext, queryReq);
        Agent agent = chatParseContext.getAgent();
        if (agent == null) {
            return queryReq;
        }
        if (agent.containsLLMParserTool()) {
            queryReq.setEnableLLM(true);
        }
        queryReq.setDataSetIds(agent.getDataSetIds());
        return queryReq;
    }

}
