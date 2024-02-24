package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaValueMap;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.core.utils.S2SqlDateHelper;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Perform SQL corrections on the "Where" section in S2SQL.
 */
@Slf4j
public class WhereCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {

        addDateIfNotExist(queryContext, semanticParseInfo);

        addQueryFilter(queryContext, semanticParseInfo);

        updateFieldValueByTechName(queryContext, semanticParseInfo);
    }

    private void addQueryFilter(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String queryFilter = getQueryFilter(queryContext.getQueryFilters());

        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();

        if (StringUtils.isNotEmpty(queryFilter)) {
            log.info("add queryFilter to correctS2SQL :{}", queryFilter);
            Expression expression = null;
            try {
                expression = CCJSqlParserUtil.parseCondExpression(queryFilter);
            } catch (JSQLParserException e) {
                log.error("parseCondExpression", e);
            }
            correctS2SQL = SqlAddHelper.addWhere(correctS2SQL, expression);
            semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctS2SQL);
        }
    }

    private void addDateIfNotExist(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        List<String> whereFields = SqlSelectHelper.getWhereFields(correctS2SQL);
        if (CollectionUtils.isEmpty(whereFields) || !TimeDimensionEnum.containsZhTimeDimension(whereFields)) {
            Pair<String, String> startEndDate = S2SqlDateHelper.getStartEndDate(queryContext,
                    semanticParseInfo.getViewId(), semanticParseInfo.getQueryType());
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

    private String getQueryFilter(QueryFilters queryFilters) {
        if (Objects.isNull(queryFilters) || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return null;
        }
        return queryFilters.getFilters().stream()
                .map(filter -> {
                    String bizNameWrap = StringUtil.getSpaceWrap(filter.getName());
                    String operatorWrap = StringUtil.getSpaceWrap(filter.getOperator().getValue());
                    String valueWrap = StringUtil.getCommaWrap(filter.getValue().toString());
                    return bizNameWrap + operatorWrap + valueWrap;
                })
                .collect(Collectors.joining(Constants.AND_UPPER));
    }

    private void updateFieldValueByTechName(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        Long viewId = semanticParseInfo.getViewId();
        List<SchemaElement> dimensions = semanticSchema.getDimensions(viewId);

        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }

        Map<String, Map<String, String>> aliasAndBizNameToTechName = getAliasAndBizNameToTechName(dimensions);
        String correctS2SQL = SqlReplaceHelper.replaceValue(semanticParseInfo.getSqlInfo().getCorrectS2SQL(),
                aliasAndBizNameToTechName);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctS2SQL);
    }

    private Map<String, Map<String, String>> getAliasAndBizNameToTechName(List<SchemaElement> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new HashMap<>();
        }

        Map<String, Map<String, String>> result = new HashMap<>();

        for (SchemaElement dimension : dimensions) {
            if (Objects.isNull(dimension)
                    || Strings.isEmpty(dimension.getName())
                    || CollectionUtils.isEmpty(dimension.getSchemaValueMaps())) {
                continue;
            }
            String name = dimension.getName();

            Map<String, String> aliasAndBizNameToTechName = new HashMap<>();

            for (SchemaValueMap valueMap : dimension.getSchemaValueMaps()) {
                if (Objects.isNull(valueMap) || Strings.isEmpty(valueMap.getTechName())) {
                    continue;
                }
                if (Strings.isNotEmpty(valueMap.getBizName())) {
                    aliasAndBizNameToTechName.put(valueMap.getBizName(), valueMap.getTechName());
                }
                if (!CollectionUtils.isEmpty(valueMap.getAlias())) {
                    valueMap.getAlias().stream().forEach(alias -> {
                        if (Strings.isNotEmpty(alias)) {
                            aliasAndBizNameToTechName.put(alias, valueMap.getTechName());
                        }
                    });
                }
            }
            if (!CollectionUtils.isEmpty(aliasAndBizNameToTechName)) {
                result.put(name, aliasAndBizNameToTechName);
            }
        }
        return result;
    }
}
