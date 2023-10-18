package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaValueMap;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.parser.llm.s2ql.S2QLDateHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

@Slf4j
public class WhereCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);

        addDateIfNotExist(semanticCorrectInfo);

        parserDateDiffFunction(semanticCorrectInfo);

        addQueryFilter(semanticCorrectInfo);

        updateFieldValueByTechName(semanticCorrectInfo);
    }

    private void addQueryFilter(SemanticCorrectInfo semanticCorrectInfo) {
        String queryFilter = getQueryFilter(semanticCorrectInfo.getQueryFilters());

        String preSql = semanticCorrectInfo.getSql();

        if (StringUtils.isNotEmpty(queryFilter)) {
            log.info("add queryFilter to preSql :{}", queryFilter);
            Expression expression = null;
            try {
                expression = CCJSqlParserUtil.parseCondExpression(queryFilter);
            } catch (JSQLParserException e) {
                log.error("parseCondExpression", e);
            }
            String sql = SqlParserAddHelper.addWhere(preSql, expression);
            semanticCorrectInfo.setSql(sql);
        }
    }

    private void parserDateDiffFunction(SemanticCorrectInfo semanticCorrectInfo) {
        String sql = semanticCorrectInfo.getSql();
        sql = SqlParserReplaceHelper.replaceFunction(sql);
        semanticCorrectInfo.setSql(sql);
    }

    private void addDateIfNotExist(SemanticCorrectInfo semanticCorrectInfo) {
        String sql = semanticCorrectInfo.getSql();
        List<String> whereFields = SqlParserSelectHelper.getWhereFields(sql);
        if (CollectionUtils.isEmpty(whereFields) || !whereFields.contains(DateUtils.DATE_FIELD)) {
            String currentDate = S2QLDateHelper.getReferenceDate(semanticCorrectInfo.getParseInfo().getModelId());
            if (StringUtils.isNotBlank(currentDate)) {
                sql = SqlParserAddHelper.addParenthesisToWhere(sql);
                sql = SqlParserAddHelper.addWhere(sql, DateUtils.DATE_FIELD, currentDate);
            }
        }
        semanticCorrectInfo.setSql(sql);
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

    private void updateFieldValueByTechName(SemanticCorrectInfo semanticCorrectInfo) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getId();
        List<SchemaElement> dimensions = semanticSchema.getDimensions().stream()
                .filter(schemaElement -> modelId.equals(schemaElement.getModel()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }

        Map<String, Map<String, String>> aliasAndBizNameToTechName = getAliasAndBizNameToTechName(dimensions);
        String sql = SqlParserReplaceHelper.replaceValue(semanticCorrectInfo.getSql(), aliasAndBizNameToTechName);
        semanticCorrectInfo.setSql(sql);
        return;
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
