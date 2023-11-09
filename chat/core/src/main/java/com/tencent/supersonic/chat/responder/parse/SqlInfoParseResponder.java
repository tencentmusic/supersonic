package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.ParseTimeCostDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class SqlInfoParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext,
            List<ChatParseDO> chatParseDOS) {
        QueryReq queryReq = queryContext.getRequest();
        Long startTime = System.currentTimeMillis();
        addSqlInfo(queryReq, parseResp.getSelectedParses());
        addSqlInfo(queryReq, parseResp.getCandidateParses());
        parseResp.setParseTimeCost(new ParseTimeCostDO());
        parseResp.getParseTimeCost().setSqlTime(System.currentTimeMillis() - startTime);
        if (!CollectionUtils.isEmpty(chatParseDOS)) {
            Map<Integer, ChatParseDO> chatParseDOMap = chatParseDOS.stream()
                    .collect(Collectors.toMap(ChatParseDO::getParseId,
                            Function.identity(), (oldValue, newValue) -> newValue));
            updateParseInfo(chatParseDOMap, parseResp.getSelectedParses());
            updateParseInfo(chatParseDOMap, parseResp.getCandidateParses());
        }
    }

    private void updateParseInfo(Map<Integer, ChatParseDO> chatParseDOMap, List<SemanticParseInfo> parseInfos) {
        if (CollectionUtils.isEmpty(parseInfos)) {
            return;
        }
        for (SemanticParseInfo parseInfo : parseInfos) {
            ChatParseDO chatParseDO = chatParseDOMap.get(parseInfo.getId());
            if (chatParseDO != null) {
                chatParseDO.setParseInfo(JsonUtil.toString(parseInfo));
            }
        }
    }

    private void addSqlInfo(QueryReq queryReq, List<SemanticParseInfo> semanticParseInfos) {
        if (CollectionUtils.isEmpty(semanticParseInfos)) {
            return;
        }
        semanticParseInfos.forEach(parseInfo -> {
            addSqlInfo(queryReq, parseInfo);
        });
    }

    private void addSqlInfo(QueryReq queryReq, SemanticParseInfo parseInfo) {
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (Objects.isNull(semanticQuery)) {
            return;
        }
        semanticQuery.setParseInfo(parseInfo);
        String explainSql = semanticQuery.explain(queryReq.getUser());
        if (StringUtils.isBlank(explainSql)) {
            return;
        }
        parseInfo.getSqlInfo().setQuerySQL(explainSql);
    }

}
