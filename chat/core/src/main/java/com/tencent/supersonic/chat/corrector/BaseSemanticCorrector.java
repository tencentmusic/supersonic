package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class BaseSemanticCorrector implements SemanticCorrector {

    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        semanticCorrectInfo.setPreSql(semanticCorrectInfo.getSql());
    }

    protected Map<String, String> getFieldNameMap(Long modelId) {

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        Map<String, String> result = dbAllFields.stream()
                .filter(entry -> entry.getModel().equals(modelId))
                .collect(Collectors.toMap(SchemaElement::getName, a -> a.getName(), (k1, k2) -> k1));
        result.put(DateUtils.DATE_FIELD, DateUtils.DATE_FIELD);
        return result;
    }

    protected void addFieldsToSelect(SemanticCorrectInfo semanticCorrectInfo, String sql) {
        Set<String> selectFields = new HashSet<>(SqlParserSelectHelper.getSelectFields(sql));
        Set<String> whereFields = new HashSet<>(SqlParserSelectHelper.getWhereFields(sql));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(whereFields)) {
            return;
        }

        whereFields.addAll(SqlParserSelectHelper.getOrderByFields(sql));
        whereFields.removeAll(selectFields);
        whereFields.remove(DateUtils.DATE_FIELD);
        String replaceFields = SqlParserUpdateHelper.addFieldsToSelect(sql, new ArrayList<>(whereFields));
        semanticCorrectInfo.setSql(replaceFields);
    }

    protected void addAggregateToMetric(SemanticCorrectInfo semanticCorrectInfo) {
        //add aggregate to all metric
        String sql = semanticCorrectInfo.getSql();
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getModel();

        List<SchemaElement> metrics = getMetricElements(modelId);

        Map<String, String> metricToAggregate = metrics.stream()
                .map(schemaElement -> {
                    if (Objects.isNull(schemaElement.getDefaultAgg())) {
                        schemaElement.setDefaultAgg(AggregateTypeEnum.SUM.name());
                    }
                    return schemaElement;
                }).collect(Collectors.toMap(a -> a.getName(), a -> a.getDefaultAgg(), (k1, k2) -> k1));

        if (CollectionUtils.isEmpty(metricToAggregate)) {
            return;
        }

        String aggregateSql = SqlParserUpdateHelper.addAggregateToField(sql, metricToAggregate);
        semanticCorrectInfo.setSql(aggregateSql);
    }

    protected List<SchemaElement> getMetricElements(Long modelId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        return semanticSchema.getMetrics(modelId);
    }

    protected List<SchemaElement> getDimensionElements(Long modelId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        return semanticSchema.getDimensions(modelId);
    }
}
