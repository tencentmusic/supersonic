package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.DateVisitor.DateBoundInfo;
import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlDateSelectHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/** Perform SQL corrections on the time in S2SQL. */
@Slf4j
public class TimeCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        if (containsPartitionDimensions(chatQueryContext, semanticParseInfo)) {
            addDateIfNotExist(chatQueryContext, semanticParseInfo);
            addLowerBoundDate(semanticParseInfo);
        } else {
            removeDateIfExist(chatQueryContext, semanticParseInfo);
        }
    }

    private void addDateIfNotExist(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        List<String> whereFields = SqlSelectHelper.getWhereFields(correctS2SQL);
        Long dataSetId = semanticParseInfo.getDataSetId();
        DataSetSchema dataSetSchema =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
        if (Objects.isNull(dataSetSchema) || Objects.isNull(dataSetSchema.getPartitionDimension())
                || Objects.isNull(dataSetSchema.getPartitionDimension().getName())) {
            return;
        }
        String partitionDimension = dataSetSchema.getPartitionDimension().getName();
        if (CollectionUtils.isEmpty(whereFields) || !whereFields.contains(partitionDimension)) {
            TimeDefaultConfig timeConfig;
            QueryConfig queryConfig = dataSetSchema.getQueryConfig();
            if (QueryType.AGGREGATE.equals(semanticParseInfo.getQueryType())) {
                timeConfig = queryConfig.getAggregateTypeDefaultConfig().getTimeDefaultConfig();
            } else {
                timeConfig = queryConfig.getDetailTypeDefaultConfig().getTimeDefaultConfig();
            }

            String timeFormat = dataSetSchema.getPartitionTimeFormat();
            Pair<String, String> dateRange =
                    S2SqlDateHelper.calculateDateRange(timeConfig, timeFormat);
            if (isValidDateRange(dateRange)) {
                correctS2SQL = SqlAddHelper.addParenthesisToWhere(correctS2SQL);
                String startDateLeft = dateRange.getLeft();
                String endDateRight = dateRange.getRight();
                String condExpr = String.format(" ( %s >= '%s'  and %s <= '%s' )",
                        partitionDimension, startDateLeft, partitionDimension, endDateRight);
                correctS2SQL = addConditionToSQL(correctS2SQL, condExpr);
            }
        }
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
    }

    private void addLowerBoundDate(SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        DateBoundInfo dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(correctS2SQL,
                semanticParseInfo.getDateInfo().getDateField());

        if (dateBoundInfo != null && StringUtils.isBlank(dateBoundInfo.getLowerBound())
                && StringUtils.isNotBlank(dateBoundInfo.getUpperBound())
                && StringUtils.isNotBlank(dateBoundInfo.getUpperDate())) {
            String upperDate = dateBoundInfo.getUpperDate();
            String condExpr = dateBoundInfo.getColumName() + " >= '" + upperDate + "'";
            correctS2SQL = addConditionToSQL(correctS2SQL, condExpr);
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
        }
    }

    private boolean isValidDateRange(Pair<String, String> startEndDate) {
        return StringUtils.isNotBlank(startEndDate.getLeft())
                && StringUtils.isNotBlank(startEndDate.getRight());
    }

    private String addConditionToSQL(String sql, String condition) {
        try {
            Expression expression = CCJSqlParserUtil.parseCondExpression(condition);
            return SqlAddHelper.addWhere(sql, expression);
        } catch (JSQLParserException e) {
            log.error("addConditionToSQL:{}", e);
            return sql;
        }
    }
}
