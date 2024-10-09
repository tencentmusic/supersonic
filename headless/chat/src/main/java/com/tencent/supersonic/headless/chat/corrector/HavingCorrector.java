package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Perform SQL corrections on the "Having" section in S2SQL. */
@Slf4j
public class HavingCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        // add aggregate to all metric
        addHaving(chatQueryContext, semanticParseInfo);
    }

    private void addHaving(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        Long dataSet = semanticParseInfo.getDataSet().getDataSetId();

        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();

        Set<String> metrics = semanticSchema.getMetrics(dataSet).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        String havingSql =
                SqlAddHelper.addHaving(semanticParseInfo.getSqlInfo().getCorrectedS2SQL(), metrics);
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(havingSql);
    }

    private void addHavingToSelect(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        if (!SqlSelectFunctionHelper.hasAggregateFunction(correctS2SQL)) {
            return;
        }
        List<Expression> havingExpressionList = SqlSelectHelper.getHavingExpression(correctS2SQL);
        if (!CollectionUtils.isEmpty(havingExpressionList)) {
            String replaceSql =
                    SqlAddHelper.addFunctionToSelect(correctS2SQL, havingExpressionList);
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(replaceSql);
        }
    }
}
