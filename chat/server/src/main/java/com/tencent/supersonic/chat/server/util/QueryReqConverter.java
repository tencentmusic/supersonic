package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import org.apache.commons.collections.MapUtils;

import java.util.Objects;

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
        if (Objects.nonNull(queryReq.getMapInfo())
                && MapUtils.isNotEmpty(queryReq.getMapInfo().getDataSetElementMatches())) {
            queryReq.setMapInfo(queryReq.getMapInfo());
        }
        return queryReq;
    }

}
