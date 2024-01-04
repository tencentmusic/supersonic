package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.springframework.util.CollectionUtils;

/**
 * Perform SQL corrections on the "Having" section in S2SQL.
 */
@Slf4j
public class HavingCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {

        //add aggregate to all metric
        addHaving(queryContext, semanticParseInfo);

        //add having expression filed to select
        addHavingToSelect(semanticParseInfo);

    }

    private void addHaving(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Set<Long> modelIds = semanticParseInfo.getModel().getModelIds();

        SemanticSchema semanticSchema = queryContext.getSemanticSchema();

        Set<String> metrics = semanticSchema.getMetrics(modelIds).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        String havingSql = SqlParserAddHelper.addHaving(semanticParseInfo.getSqlInfo().getCorrectS2SQL(), metrics);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(havingSql);
    }

    private void addHavingToSelect(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        if (!SqlParserSelectFunctionHelper.hasAggregateFunction(correctS2SQL)) {
            return;
        }
        List<Expression> havingExpressionList = SqlParserSelectHelper.getHavingExpression(correctS2SQL);
        if (!CollectionUtils.isEmpty(havingExpressionList)) {
            String replaceSql = SqlParserAddHelper.addFunctionToSelect(correctS2SQL, havingExpressionList);
            semanticParseInfo.getSqlInfo().setCorrectS2SQL(replaceSql);
        }
        return;
    }

}
