package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class BaseSemanticCorrector implements SemanticCorrector {

    public void correct(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {
        try {
            if (StringUtils.isBlank(semanticParseInfo.getSqlInfo().getCorrectS2SQL())) {
                return;
            }
            doCorrect(queryReq, semanticParseInfo);
            log.info("sqlCorrection:{} sql:{}", this.getClass().getSimpleName(), semanticParseInfo.getSqlInfo());
        } catch (Exception e) {
            log.error(String.format("correct error,sqlInfo:%s", semanticParseInfo.getSqlInfo()), e);
        }
    }


    public abstract void doCorrect(QueryReq queryReq, SemanticParseInfo semanticParseInfo);

    protected Map<String, String> getFieldNameMap(Long modelId) {

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        // support fieldName and field alias
        Map<String, String> result = dbAllFields.stream()
                .filter(entry -> entry.getModel().equals(modelId))
                .flatMap(schemaElement -> {
                    Set<String> elements = new HashSet<>();
                    elements.add(schemaElement.getName());
                    if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                        elements.addAll(schemaElement.getAlias());
                    }
                    return elements.stream();
                })
                .collect(Collectors.toMap(a -> a, a -> a, (k1, k2) -> k1));
        result.put(TimeDimensionEnum.DAY.getChName(), TimeDimensionEnum.DAY.getChName());
        result.put(TimeDimensionEnum.MONTH.getChName(), TimeDimensionEnum.MONTH.getChName());
        result.put(TimeDimensionEnum.WEEK.getChName(), TimeDimensionEnum.WEEK.getChName());

        result.put(TimeDimensionEnum.DAY.getName(), TimeDimensionEnum.DAY.getChName());
        result.put(TimeDimensionEnum.MONTH.getName(), TimeDimensionEnum.MONTH.getChName());
        result.put(TimeDimensionEnum.WEEK.getName(), TimeDimensionEnum.WEEK.getChName());

        return result;
    }

    protected void addFieldsToSelect(SemanticParseInfo semanticParseInfo, String logicSql) {
        Set<String> selectFields = new HashSet<>(SqlParserSelectHelper.getSelectFields(logicSql));
        Set<String> needAddFields = new HashSet<>(SqlParserSelectHelper.getGroupByFields(logicSql));
        needAddFields.addAll(SqlParserSelectHelper.getOrderByFields(logicSql));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(needAddFields)) {
            return;
        }

        needAddFields.removeAll(selectFields);
        needAddFields.remove(TimeDimensionEnum.DAY.getChName());
        String replaceFields = SqlParserAddHelper.addFieldsToSelect(logicSql, new ArrayList<>(needAddFields));
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(replaceFields);
    }

    protected void addAggregateToMetric(SemanticParseInfo semanticParseInfo) {
        //add aggregate to all metric
        String logicSql = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        Long modelId = semanticParseInfo.getModel().getModel();

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
        String aggregateSql = SqlParserAddHelper.addAggregateToField(logicSql, metricToAggregate);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(aggregateSql);
    }

    protected List<SchemaElement> getMetricElements(Long modelId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        return semanticSchema.getMetrics(modelId);
    }

}
