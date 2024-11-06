package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.utils.QueryFilterParser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Perform SQL corrections on the "Where" section in S2SQL. */
@Slf4j
public class WhereCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        addQueryFilter(chatQueryContext, semanticParseInfo);
        updateFieldValueByTechName(chatQueryContext, semanticParseInfo);
    }

    protected void addQueryFilter(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        String queryFilter = getQueryFilter(chatQueryContext.getRequest().getQueryFilters());
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();

        if (StringUtils.isNotEmpty(queryFilter)) {
            log.info("add queryFilter to correctS2SQL :{}", queryFilter);
            try {
                Expression expression = CCJSqlParserUtil.parseCondExpression(queryFilter);
                correctS2SQL = SqlAddHelper.addWhere(correctS2SQL, expression);
                semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
            } catch (JSQLParserException e) {
                log.error("parseCondExpression", e);
            }
        }
    }

    private String getQueryFilter(QueryFilters queryFilters) {
        if (Objects.isNull(queryFilters) || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return null;
        }
        return QueryFilterParser.parse(queryFilters);
    }

    private void updateFieldValueByTechName(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        Long dataSetId = semanticParseInfo.getDataSetId();
        List<SchemaElement> dimensions = semanticSchema.getDimensions(dataSetId);

        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        Map<String, Map<String, String>> aliasAndBizNameToTechName =
                getAliasAndBizNameToTechName(dimensions);
        String correctedS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        String replaceSql =
                SqlReplaceHelper.replaceValue(correctedS2SQL, aliasAndBizNameToTechName);
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(replaceSql);
    }

    private Map<String, Map<String, String>> getAliasAndBizNameToTechName(
            List<SchemaElement> dimensions) {
        return dimensions.stream()
                .filter(dimension -> Objects.nonNull(dimension)
                        && StringUtils.isNotEmpty(dimension.getName())
                        && !CollectionUtils.isEmpty(dimension.getSchemaValueMaps()))
                .collect(Collectors.toMap(SchemaElement::getName,
                        dimension -> dimension.getSchemaValueMaps().stream()
                                .filter(valueMap -> Objects.nonNull(valueMap)
                                        && StringUtils.isNotEmpty(valueMap.getTechName()))
                                .flatMap(valueMap -> {
                                    Map<String, String> map = new HashMap<>();
                                    if (StringUtils.isNotEmpty(valueMap.getBizName())) {
                                        map.put(valueMap.getBizName(), valueMap.getTechName());
                                    }
                                    if (!CollectionUtils.isEmpty(valueMap.getAlias())) {
                                        valueMap.getAlias().stream().filter(StringUtils::isNotEmpty)
                                                .forEach(alias -> map.put(alias,
                                                        valueMap.getTechName()));
                                    }
                                    return map.entrySet().stream();
                                }).collect(
                                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }
}
