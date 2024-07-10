package com.tencent.supersonic.headless.chat.corrector;


import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaValueMap;
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

/**
 * Perform SQL corrections on the "Where" section in S2SQL.
 */
@Slf4j
public class WhereCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {

        addQueryFilter(chatQueryContext, semanticParseInfo);

        updateFieldValueByTechName(chatQueryContext, semanticParseInfo);
    }

    protected void addQueryFilter(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        String queryFilter = getQueryFilter(chatQueryContext.getQueryFilters());

        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();

        if (StringUtils.isNotEmpty(queryFilter)) {
            log.info("add queryFilter to correctS2SQL :{}", queryFilter);
            Expression expression = null;
            try {
                expression = CCJSqlParserUtil.parseCondExpression(queryFilter);
            } catch (JSQLParserException e) {
                log.error("parseCondExpression", e);
            }
            correctS2SQL = SqlAddHelper.addWhere(correctS2SQL, expression);
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
        }
    }

    private String getQueryFilter(QueryFilters queryFilters) {
        if (Objects.isNull(queryFilters) || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return null;
        }
        return QueryFilterParser.parse(queryFilters);
    }

    private void updateFieldValueByTechName(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        Long dataSetId = semanticParseInfo.getDataSetId();
        List<SchemaElement> dimensions = semanticSchema.getDimensions(dataSetId);

        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }

        Map<String, Map<String, String>> aliasAndBizNameToTechName = getAliasAndBizNameToTechName(dimensions);
        String correctS2SQL = SqlReplaceHelper.replaceValue(semanticParseInfo.getSqlInfo().getCorrectedS2SQL(),
                aliasAndBizNameToTechName);
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
    }

    private Map<String, Map<String, String>> getAliasAndBizNameToTechName(List<SchemaElement> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new HashMap<>();
        }

        Map<String, Map<String, String>> result = new HashMap<>();

        for (SchemaElement dimension : dimensions) {
            if (Objects.isNull(dimension)
                    || StringUtils.isEmpty(dimension.getName())
                    || CollectionUtils.isEmpty(dimension.getSchemaValueMaps())) {
                continue;
            }
            String name = dimension.getName();

            Map<String, String> aliasAndBizNameToTechName = new HashMap<>();

            for (SchemaValueMap valueMap : dimension.getSchemaValueMaps()) {
                if (Objects.isNull(valueMap) || StringUtils.isEmpty(valueMap.getTechName())) {
                    continue;
                }
                if (StringUtils.isNotEmpty(valueMap.getBizName())) {
                    aliasAndBizNameToTechName.put(valueMap.getBizName(), valueMap.getTechName());
                }
                if (!CollectionUtils.isEmpty(valueMap.getAlias())) {
                    valueMap.getAlias().stream().forEach(alias -> {
                        if (StringUtils.isNotEmpty(alias)) {
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
