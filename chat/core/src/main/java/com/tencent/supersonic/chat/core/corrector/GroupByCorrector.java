package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * Perform SQL corrections on the "Group by" section in S2SQL.
 */
@Slf4j
public class GroupByCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {

        addGroupByFields(queryContext, semanticParseInfo);

    }

    private void addGroupByFields(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Set<Long> modelIds = semanticParseInfo.getModel().getModelIds();

        //add dimension group by
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String correctS2SQL = sqlInfo.getCorrectS2SQL();
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        // check if has distinct
        boolean hasDistinct = SqlParserSelectHelper.hasDistinct(correctS2SQL);
        if (hasDistinct) {
            log.info("not add group by ,exist distinct in correctS2SQL:{}", correctS2SQL);
            return;
        }
        //add alias field name
        Set<String> dimensions = semanticSchema.getDimensions(modelIds).stream()
                .flatMap(
                        schemaElement -> {
                            Set<String> elements = new HashSet<>();
                            elements.add(schemaElement.getName());
                            if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                                elements.addAll(schemaElement.getAlias());
                            }
                            return elements.stream();
                        }
                ).collect(Collectors.toSet());
        dimensions.add(TimeDimensionEnum.DAY.getChName());

        List<String> selectFields = SqlParserSelectHelper.getSelectFields(correctS2SQL);

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        // if only date in select not add group by.
        if (selectFields.size() == 1 && selectFields.contains(TimeDimensionEnum.DAY.getChName())) {
            return;
        }
        if (SqlParserSelectHelper.hasGroupBy(correctS2SQL)) {
            log.info("not add group by ,exist group by in correctS2SQL:{}", correctS2SQL);
            return;
        }

        List<String> aggregateFields = SqlParserSelectHelper.getAggregateFields(correctS2SQL);
        Set<String> groupByFields = selectFields.stream()
                .filter(field -> dimensions.contains(field))
                .filter(field -> {
                    if (!CollectionUtils.isEmpty(aggregateFields) && aggregateFields.contains(field)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(SqlParserAddHelper.addGroupBy(correctS2SQL, groupByFields));

        addAggregate(queryContext, semanticParseInfo);
    }

    private void addAggregate(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        List<String> sqlGroupByFields = SqlParserSelectHelper.getGroupByFields(
                semanticParseInfo.getSqlInfo().getCorrectS2SQL());
        if (CollectionUtils.isEmpty(sqlGroupByFields)) {
            return;
        }
        addAggregateToMetric(queryContext, semanticParseInfo);
    }
}
