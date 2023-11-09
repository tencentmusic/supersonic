package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;

import java.util.List;
import java.util.ArrayList;
import java.util.OptionalDouble;

import com.tencent.supersonic.chat.query.rule.metric.MetricEntityQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricModelQuery;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HeuristicQuerySelector implements QuerySelector {

    @Override
    public List<SemanticQuery> select(List<SemanticQuery> candidateQueries, QueryReq queryReq) {
        log.debug("pick before [{}]", candidateQueries.stream().collect(Collectors.toList()));
        List<SemanticQuery> selectedQueries = new ArrayList<>();
        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        Double candidateThreshold = optimizationConfig.getCandidateThreshold();
        if (CollectionUtils.isNotEmpty(candidateQueries) && candidateQueries.size() == 1) {
            selectedQueries.addAll(candidateQueries);
        } else {
            OptionalDouble maxScoreOp = candidateQueries.stream().mapToDouble(
                    q -> q.getParseInfo().getScore()).max();
            if (maxScoreOp.isPresent()) {
                double maxScore = maxScoreOp.getAsDouble();

                candidateQueries.stream().forEach(query -> {
                    SemanticParseInfo parseInfo = query.getParseInfo();
                    if (!checkFullyInherited(query)
                            && (maxScore - parseInfo.getScore()) / maxScore <= candidateThreshold
                            && checkSatisfyOtherRules(query, candidateQueries)) {
                        selectedQueries.add(query);
                    }
                    log.info("candidate query (Model={}, queryMode={}) with score={}",
                            parseInfo.getModelName(), parseInfo.getQueryMode(), parseInfo.getScore());
                });
            }
        }
        log.debug("pick after [{}]", selectedQueries.stream().collect(Collectors.toList()));
        return selectedQueries;
    }

    private boolean checkSatisfyOtherRules(SemanticQuery semanticQuery, List<SemanticQuery> candidateQueries) {
        if (!semanticQuery.getQueryMode().equals(MetricModelQuery.QUERY_MODE)) {
            return true;
        }
        for (SemanticQuery candidateQuery : candidateQueries) {
            if (candidateQuery.getQueryMode().equals(MetricEntityQuery.QUERY_MODE)
                    && semanticQuery.getParseInfo().getScore() == candidateQuery.getParseInfo().getScore()) {
                return false;
            }
        }
        return true;
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
        if (parseInfo.getDateInfo() != null && !parseInfo.getDateInfo().isInherited()) {
            return false;
        }

        return true;
    }
}
