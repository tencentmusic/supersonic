package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
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
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);

        addGroupByFields(semanticCorrectInfo);

    }

    private void addGroupByFields(SemanticCorrectInfo semanticCorrectInfo) {
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getModel();

        //add dimension group by
        String sql = semanticCorrectInfo.getSql();
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
        dimensions.add(DateUtils.DATE_FIELD);

        List<String> selectFields = SqlParserSelectHelper.getSelectFields(sql);

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        // if only date in select not add group by.
        if (selectFields.size() == 1 && selectFields.contains(DateUtils.DATE_FIELD)) {
            return;
        }
        if (SqlParserSelectHelper.hasGroupBy(sql)) {
            log.info("not add group by ,exist group by in sql:{}", sql);
            return;
        }

        List<String> aggregateFields = SqlParserSelectHelper.getAggregateFields(sql);
        Set<String> groupByFields = selectFields.stream()
                .filter(field -> dimensions.contains(field))
                .filter(field -> {
                    if (!CollectionUtils.isEmpty(aggregateFields) && aggregateFields.contains(field)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        semanticCorrectInfo.setSql(SqlParserAddHelper.addGroupBy(sql, groupByFields));

        addAggregate(semanticCorrectInfo);
    }

    private void addAggregate(SemanticCorrectInfo semanticCorrectInfo) {
        List<String> sqlGroupByFields = SqlParserSelectHelper.getGroupByFields(semanticCorrectInfo.getSql());
        if (CollectionUtils.isEmpty(sqlGroupByFields)) {
            return;
        }
        addAggregateToMetric(semanticCorrectInfo);
    }
}
