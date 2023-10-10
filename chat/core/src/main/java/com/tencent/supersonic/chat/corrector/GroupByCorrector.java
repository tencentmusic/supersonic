package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
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
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getModel();
        // if select not exit metric not add aggregate
        List<String> selectFields = SqlParserSelectHelper.getSelectFields(semanticCorrectInfo.getSql());

        Set<String> metrics = getMetricElements(modelId).stream()
                .map(schemaElement -> schemaElement.getName())
                .collect(Collectors.toSet());

        if (!CollectionUtils.isEmpty(selectFields)
                && !CollectionUtils.isEmpty(metrics)
                && selectFields.stream().anyMatch(s -> metrics.contains(s))) {
            //add aggregate to all metric
            addAggregateToMetric(semanticCorrectInfo);
        }

        //add dimension group by
        String sql = semanticCorrectInfo.getSql();
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        Set<String> dimensions = semanticSchema.getDimensions(modelId).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());
        dimensions.add(DateUtils.DATE_FIELD);
        selectFields = SqlParserSelectHelper.getSelectFields(sql);

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(dimensions)) {
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
        semanticCorrectInfo.setSql(SqlParserUpdateHelper.addGroupBy(sql, groupByFields));
    }
}
