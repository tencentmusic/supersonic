package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.QueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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
        correctors.add(new AggCorrector());
        correctors.add(new HavingCorrector());
    }

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        for (BaseSemanticCorrector corrector : correctors) {
            corrector.correct(queryContext, semanticParseInfo);
        }
        removeSameFieldFromSelect(semanticParseInfo);
    }

    public void removeSameFieldFromSelect(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        correctS2SQL = SqlRemoveHelper.removeSameFieldFromSelect(correctS2SQL);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctS2SQL);
    }
}
