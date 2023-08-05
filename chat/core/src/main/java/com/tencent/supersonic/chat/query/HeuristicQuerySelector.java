package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.common.pojo.Constants;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HeuristicQuerySelector implements QuerySelector {

    private static final double MATCH_INHERIT_PENALTY = 0.5;
    private static final double MATCH_CURRENT_REWORD = 2;
    private static final double CANDIDATE_THRESHOLD = 0.2;

    @Override
    public List<SemanticQuery> select(List<SemanticQuery> candidateQueries) {
        List<SemanticQuery> selectedQueries = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(candidateQueries) && candidateQueries.size() == 1) {
            selectedQueries.addAll(candidateQueries);
        } else {
            OptionalDouble maxScoreOp = candidateQueries.stream().mapToDouble(
                    q -> computeScore(q.getParseInfo())).max();
            if (maxScoreOp.isPresent()) {
                double maxScore = maxScoreOp.getAsDouble();

                candidateQueries.stream().forEach(query -> {
                    SemanticParseInfo semanticParse = query.getParseInfo();
                    if ((maxScore - semanticParse.getScore()) / maxScore <= CANDIDATE_THRESHOLD) {
                        selectedQueries.add(query);
                    }
                    log.info("candidate query (domain={}, queryMode={}) with score={}",
                            semanticParse.getDomainName(), semanticParse.getQueryMode(), semanticParse.getScore());
                });
            }
        }
        return selectedQueries;
    }

    private double computeScore(SemanticParseInfo semanticParse) {
        double totalScore = 0;

        Map<SchemaElementType, SchemaElementMatch> maxSimilarityMatch = new HashMap<>();
        for (SchemaElementMatch match : semanticParse.getElementMatches()) {
            SchemaElementType type = match.getElement().getType();
            if (!maxSimilarityMatch.containsKey(type) ||
                    match.getSimilarity() > maxSimilarityMatch.get(type).getSimilarity()) {
                maxSimilarityMatch.put(type, match);
            }
        }

        for (SchemaElementMatch match : maxSimilarityMatch.values()) {
            double matchScore = Optional.ofNullable(match.getDetectWord()).orElse(Constants.EMPTY).length() * match.getSimilarity();
            if (match.equals(SchemaElementMatch.MatchMode.INHERIT)) {
                matchScore *= MATCH_INHERIT_PENALTY;
            } else {
                matchScore *= MATCH_CURRENT_REWORD;
            }
            totalScore += matchScore;
        }

        // original score in parse info acts like an extra bonus
        totalScore += semanticParse.getScore();
        semanticParse.setScore(totalScore);

        return totalScore;
    }
}
