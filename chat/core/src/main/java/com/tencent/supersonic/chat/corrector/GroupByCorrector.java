package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class GroupByCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {

        addGroupByFields(semanticParseInfo);

    }

    private void addGroupByFields(SemanticParseInfo semanticParseInfo) {
        Long modelId = semanticParseInfo.getModel().getModel();

        //add dimension group by
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String logicSql = sqlInfo.getCorrectS2SQL();
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        //add alias field name
        Set<String> dimensions = semanticSchema.getDimensions(modelId).stream()
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

        List<String> selectFields = SqlParserSelectHelper.getSelectFields(logicSql);

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        // if only date in select not add group by.
        if (selectFields.size() == 1 && selectFields.contains(TimeDimensionEnum.DAY.getChName())) {
            return;
        }
        if (SqlParserSelectHelper.hasGroupBy(logicSql)) {
            log.info("not add group by ,exist group by in logicSql:{}", logicSql);
            return;
        }

        List<String> aggregateFields = SqlParserSelectHelper.getAggregateFields(logicSql);
        Set<String> groupByFields = selectFields.stream()
                .filter(field -> dimensions.contains(field))
                .filter(field -> {
                    if (!CollectionUtils.isEmpty(aggregateFields) && aggregateFields.contains(field)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(SqlParserAddHelper.addGroupBy(logicSql, groupByFields));

        addAggregate(semanticParseInfo);
    }

    private void addAggregate(SemanticParseInfo semanticParseInfo) {
        List<String> sqlGroupByFields = SqlParserSelectHelper.getGroupByFields(
                semanticParseInfo.getSqlInfo().getCorrectS2SQL());
        if (CollectionUtils.isEmpty(sqlGroupByFields)) {
            return;
        }
        addAggregateToMetric(semanticParseInfo);
    }
}
