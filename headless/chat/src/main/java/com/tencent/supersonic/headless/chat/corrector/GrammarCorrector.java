package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
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
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        for (BaseSemanticCorrector corrector : correctors) {
            corrector.correct(chatQueryContext, semanticParseInfo);
        }
        removeSameFieldFromSelect(semanticParseInfo);
    }

    public void removeSameFieldFromSelect(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        correctS2SQL = SqlRemoveHelper.removeSameFieldFromSelect(correctS2SQL);
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
    }
}
