package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Correcting SQL syntax, primarily including fixes to select, where, groupBy, and Having clauses
 */
@Slf4j
public class GrammarCorrector extends BaseSemanticCorrector {

    private List<BaseSemanticCorrector> correctors;

    public GrammarCorrector() {
        correctors = new ArrayList<>();
        correctors.add(new SelectCorrector());
        correctors.add(new WhereCorrector());
        correctors.add(new GroupByCorrector());
        correctors.add(new HavingCorrector());
    }

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        for (BaseSemanticCorrector corrector : correctors) {
            corrector.correct(queryContext, semanticParseInfo);
        }
    }
}
