package com.tencent.supersonic.headless.core.chat.corrector;


import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.DateVisitor.DateBoundInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlDateSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.utils.S2SqlDateHelper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * Perform SQL corrections on the time in S2SQL.
 */
@Slf4j
public class TimeCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {

        addDateIfNotExist(queryContext, semanticParseInfo);

        parserDateDiffFunction(semanticParseInfo);

        addLowerBoundDate(semanticParseInfo);

    }

    private void addDateIfNotExist(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        List<String> whereFields = SqlSelectHelper.getWhereFields(correctS2SQL);
        if (CollectionUtils.isEmpty(whereFields) || !TimeDimensionEnum.containsZhTimeDimension(whereFields)) {

            Pair<String, String> startEndDate = S2SqlDateHelper.getStartEndDate(queryContext,
                    semanticParseInfo.getDataSetId(), semanticParseInfo.getQueryType());

            if (StringUtils.isNotBlank(startEndDate.getLeft())
                    && StringUtils.isNotBlank(startEndDate.getRight())) {
                correctS2SQL = SqlAddHelper.addParenthesisToWhere(correctS2SQL);
                String dateChName = TimeDimensionEnum.DAY.getChName();
                String condExpr = String.format(" ( %s >= '%s'  and %s <= '%s' )", dateChName,
                        startEndDate.getLeft(), dateChName, startEndDate.getRight());
                try {
                    Expression expression = CCJSqlParserUtil.parseCondExpression(condExpr);
                    correctS2SQL = SqlAddHelper.addWhere(correctS2SQL, expression);
                } catch (JSQLParserException e) {
                    log.error("parseCondExpression:{}", e);
                }
            }
        }
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctS2SQL);
    }

    private void addLowerBoundDate(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        DateBoundInfo dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(correctS2SQL);
        if (Objects.isNull(dateBoundInfo)) {
            return;
        }
        if (StringUtils.isBlank(dateBoundInfo.getLowerBound())
                && StringUtils.isNotBlank(dateBoundInfo.getUpperBound())
                && StringUtils.isNotBlank(dateBoundInfo.getUpperDate())) {
            String upperDate = dateBoundInfo.getUpperDate();
            try {
                correctS2SQL = SqlAddHelper.addParenthesisToWhere(correctS2SQL);
                String condExpr = dateBoundInfo.getColumName() + " >= '" + upperDate + "'";
                correctS2SQL = SqlAddHelper.addWhere(correctS2SQL, CCJSqlParserUtil.parseCondExpression(condExpr));
            } catch (JSQLParserException e) {
                log.error("parseCondExpression", e);
            }
            semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctS2SQL);
        }
    }

    private void parserDateDiffFunction(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        correctS2SQL = SqlReplaceHelper.replaceFunction(correctS2SQL);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctS2SQL);
    }

}
