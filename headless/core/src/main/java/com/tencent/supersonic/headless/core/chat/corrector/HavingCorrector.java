package com.tencent.supersonic.headless.core.chat.corrector;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perform SQL corrections on the "Having" section in S2SQL.
 */
@Slf4j
public class HavingCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {

        //add aggregate to all metric
        addHaving(queryContext, semanticParseInfo);

        //decide whether add having expression field to select
        Environment environment = ContextUtils.getBean(Environment.class);
        String correctorAdditionalInfo = environment.getProperty("s2.corrector.additional.information");
        if (StringUtils.isNotBlank(correctorAdditionalInfo) && Boolean.parseBoolean(correctorAdditionalInfo)) {
            addHavingToSelect(semanticParseInfo);
        }

    }

    private void addHaving(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Long dataSet = semanticParseInfo.getDataSet().getDataSet();

        SemanticSchema semanticSchema = queryContext.getSemanticSchema();

        Set<String> metrics = semanticSchema.getMetrics(dataSet).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        String havingSql = SqlAddHelper.addHaving(semanticParseInfo.getSqlInfo().getCorrectS2SQL(), metrics);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(havingSql);
    }

    private void addHavingToSelect(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        if (!SqlSelectFunctionHelper.hasAggregateFunction(correctS2SQL)) {
            return;
        }
        List<Expression> havingExpressionList = SqlSelectHelper.getHavingExpression(correctS2SQL);
        if (!CollectionUtils.isEmpty(havingExpressionList)) {
            String replaceSql = SqlAddHelper.addFunctionToSelect(correctS2SQL, havingExpressionList);
            semanticParseInfo.getSqlInfo().setCorrectS2SQL(replaceSql);
        }
        return;
    }

}
