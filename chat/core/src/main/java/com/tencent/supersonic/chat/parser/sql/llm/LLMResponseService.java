package com.tencent.supersonic.chat.parser.sql.llm;

import com.tencent.supersonic.chat.agent.NL2SQLTool;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.LLMSemanticQuery;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserEqualHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class LLMResponseService {

    public SemanticParseInfo addParseInfo(QueryContext queryCtx, ParseResult parseResult, String s2SQL, Double weight) {
        if (Objects.isNull(weight)) {
            weight = 0D;
        }
        LLMSemanticQuery semanticQuery = QueryManager.createLLMQuery(LLMSqlQuery.QUERY_MODE);
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        parseInfo.setModel(parseResult.getModelCluster());
        NL2SQLTool commonAgentTool = parseResult.getCommonAgentTool();
        parseInfo.getElementMatches().addAll(queryCtx.getModelClusterMapInfo()
                .getMatchedElements(parseInfo.getModelClusterKey()));

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, parseResult);
        properties.put("type", "internal");
        properties.put("name", commonAgentTool.getName());

        parseInfo.setProperties(properties);
        parseInfo.setScore(queryCtx.getRequest().getQueryText().length() * (1 + weight));
        parseInfo.setQueryMode(semanticQuery.getQueryMode());
        parseInfo.getSqlInfo().setS2SQL(s2SQL);
        parseInfo.setModel(parseResult.getModelCluster());
        queryCtx.getCandidateQueries().add(semanticQuery);
        return parseInfo;
    }

    public Map<String, Double> getDeduplicationSqlWeight(LLMResp llmResp) {
        if (MapUtils.isEmpty(llmResp.getSqlWeight())) {
            return llmResp.getSqlWeight();
        }
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : llmResp.getSqlWeight().entrySet()) {
            String key = entry.getKey();
            if (result.keySet().stream().anyMatch(existKey -> SqlParserEqualHelper.equals(existKey, key))) {
                continue;
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
