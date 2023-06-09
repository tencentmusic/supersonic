package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.common.constant.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeuristicQuerySelector implements QuerySelector {

    @Override
    public SemanticQuery select(List<SemanticQuery> candidateQueries) {
        double maxScore = 0;
        SemanticQuery pickedQuery = null;

        for (SemanticQuery query : candidateQueries) {
            if (query instanceof LLMSemanticQuery) {
                log.info("force to use LLM if existed");
                return query;
            }

            SemanticParseInfo semanticParse = query.getParseInfo();
            double score = computeScore(semanticParse);
            if (score > maxScore) {
                maxScore = score;
                pickedQuery = query;
            }
            log.info("candidate query (domain={}, queryMode={}) with score={}",
                    semanticParse.getDomainName(), semanticParse.getQueryMode(), score);
        }

        return pickedQuery;
    }

    private double computeScore(SemanticParseInfo semanticParse) {
        double score = 0;

        Map<SchemaElementType, SchemaElementMatch> maxSimilarityMatch = new HashMap<>();
        for (SchemaElementMatch match : semanticParse.getElementMatches()) {
            SchemaElementType type = match.getElementType();
            if (!maxSimilarityMatch.containsKey(type) ||
                    match.getSimilarity() > maxSimilarityMatch.get(type).getSimilarity()) {
                maxSimilarityMatch.put(type, match);
            }
        }

        for (SchemaElementMatch match : maxSimilarityMatch.values()) {
            score +=
                    Optional.ofNullable(match.getDetectWord()).orElse(Constants.EMPTY).length() * match.getSimilarity();
        }

        // bonus is a special construct to control the final score
        score += semanticParse.getBonus();

        return score;
    }
}
