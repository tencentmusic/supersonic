package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.jsqlparser.SqlEqualHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.query.QueryManager;
import com.tencent.supersonic.headless.core.chat.query.llm.LLMSemanticQuery;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlResp;
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
        parseInfo.setDataSet(queryCtx.getSemanticSchema().getDataSet(parseResult.getDataSetId()));
        parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(parseInfo.getDataSetId()));

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, parseResult);
        properties.put("type", "internal");
        parseInfo.setProperties(properties);
        parseInfo.setScore(queryCtx.getQueryText().length() * (1 + weight));
        parseInfo.setQueryMode(semanticQuery.getQueryMode());
        parseInfo.getSqlInfo().setS2SQL(s2SQL);
        queryCtx.getCandidateQueries().add(semanticQuery);
        return parseInfo;
    }

    public Map<String, LLMSqlResp> getDeduplicationSqlResp(LLMResp llmResp) {
        if (MapUtils.isEmpty(llmResp.getSqlRespMap())) {
            return llmResp.getSqlRespMap();
        }
        Map<String, LLMSqlResp> result = new HashMap<>();
        for (Map.Entry<String, LLMSqlResp> entry : llmResp.getSqlRespMap().entrySet()) {
            String key = entry.getKey();
            if (result.keySet().stream().anyMatch(existKey -> SqlEqualHelper.equals(existKey, key))) {
                continue;
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
