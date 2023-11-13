package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QueryRanker {

    @Value("${candidate.top.size:5}")
    private int candidateTopSize;

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
