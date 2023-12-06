package com.tencent.supersonic.chat.processor;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QueryRankProcessor ranks candidate parsing results based on
 * a heuristic scoring algorithm and then takes topN.
 **/
@Slf4j
public class QueryRankProcessor implements ParseResultProcessor {

    private static final int candidateTopSize = 5;

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        candidateQueries = rank(candidateQueries);
        queryContext.setCandidateQueries(candidateQueries);
    }

    public List<SemanticQuery> rank(List<SemanticQuery> candidateQueries) {
        log.debug("pick before [{}]", candidateQueries);
        if (CollectionUtils.isEmpty(candidateQueries)) {
            return candidateQueries;
        }
        List<SemanticQuery> selectedQueries = new ArrayList<>();
        if (candidateQueries.size() == 1) {
            selectedQueries.addAll(candidateQueries);
        } else {
            selectedQueries = getTopCandidateQuery(candidateQueries);
        }
        generateParseInfoId(selectedQueries);
        log.debug("pick after [{}]", selectedQueries);
        return selectedQueries;
    }

    public List<SemanticQuery> getTopCandidateQuery(List<SemanticQuery> semanticQueries) {
        return semanticQueries.stream()
                .filter(query -> !checkFullyInherited(query))
                .sorted((o1, o2) -> {
                    if (o1.getParseInfo().getScore() < o2.getParseInfo().getScore()) {
                        return 1;
                    } else if (o1.getParseInfo().getScore() > o2.getParseInfo().getScore()) {
                        return -1;
                    }
                    return 0;
                }).limit(candidateTopSize)
                .collect(Collectors.toList());
    }

    private void generateParseInfoId(List<SemanticQuery> semanticQueries) {
        for (int i = 0; i < semanticQueries.size(); i++) {
            SemanticQuery query = semanticQueries.get(i);
            query.getParseInfo().setId(i + 1);
        }
    }

    private boolean checkFullyInherited(SemanticQuery query) {
        SemanticParseInfo parseInfo = query.getParseInfo();
        if (!(query instanceof RuleSemanticQuery)) {
            return false;
        }

        for (SchemaElementMatch match : parseInfo.getElementMatches()) {
            if (!match.isInherited()) {
                return false;
            }
        }
        return parseInfo.getDateInfo() == null || parseInfo.getDateInfo().isInherited();
    }
}
