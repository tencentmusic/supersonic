package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * basic semantic correction functionality, offering common methods and an abstract method called
 * doCorrect
 */
@Slf4j
public abstract class BaseSemanticCorrector implements SemanticCorrector {

    public void correct(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        try {
            if (StringUtils.isBlank(semanticParseInfo.getSqlInfo().getCorrectedS2SQL())) {
                return;
            }
            doCorrect(chatQueryContext, semanticParseInfo);
            log.debug("sqlCorrection:{} sql:{}", this.getClass().getSimpleName(),
                    semanticParseInfo.getSqlInfo());
        } catch (Exception e) {
            log.error(String.format("correct error,sqlInfo:%s", semanticParseInfo.getSqlInfo()), e);
        }
    }

    public abstract void doCorrect(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo);

    protected Map<String, String> getFieldNameMap(ChatQueryContext chatQueryContext,
            Long dataSetId) {

        Map<String, String> result = getFieldNameMapFromDB(chatQueryContext, dataSetId);
        if (chatQueryContext.containsPartitionDimensions(dataSetId)) {
            result.put(TimeDimensionEnum.DAY.getChName(), TimeDimensionEnum.DAY.getChName());
            result.put(TimeDimensionEnum.MONTH.getChName(), TimeDimensionEnum.MONTH.getChName());
            result.put(TimeDimensionEnum.WEEK.getChName(), TimeDimensionEnum.WEEK.getChName());

            result.put(TimeDimensionEnum.DAY.getName(), TimeDimensionEnum.DAY.getChName());
            result.put(TimeDimensionEnum.MONTH.getName(), TimeDimensionEnum.MONTH.getChName());
            result.put(TimeDimensionEnum.WEEK.getName(), TimeDimensionEnum.WEEK.getChName());
        }
        return result;
    }

    private static Map<String, String> getFieldNameMapFromDB(ChatQueryContext chatQueryContext,
            Long dataSetId) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();

        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        // support fieldName and field alias
        return dbAllFields.stream().filter(entry -> dataSetId.equals(entry.getDataSetId()))
                .flatMap(schemaElement -> {
                    Set<String> elements = new HashSet<>();
                    elements.add(schemaElement.getName());
                    if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                        elements.addAll(schemaElement.getAlias());
                    }
                    return elements.stream();
                }).collect(Collectors.toMap(a -> a, a -> a, (k1, k2) -> k1));
    }

    protected void addAggregateToMetric(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        // add aggregate to all metric
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        Long dataSetId = semanticParseInfo.getDataSet().getDataSetId();
        List<SchemaElement> metrics = getMetricElements(chatQueryContext, dataSetId);

        Map<String, String> metricToAggregate = metrics.stream().map(schemaElement -> {
            if (Objects.isNull(schemaElement.getDefaultAgg())) {
                schemaElement.setDefaultAgg(AggregateTypeEnum.SUM.name());
            }
            return schemaElement;
        }).flatMap(schemaElement -> {
            Set<String> elements = new HashSet<>();
            elements.add(schemaElement.getName());
            if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                elements.addAll(schemaElement.getAlias());
            }
            return elements.stream()
                    .map(element -> Pair.of(element, schemaElement.getDefaultAgg()));
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        if (CollectionUtils.isEmpty(metricToAggregate)) {
            return;
        }
        String aggregateSql = SqlAddHelper.addAggregateToField(correctS2SQL, metricToAggregate);
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(aggregateSql);
    }

    protected List<SchemaElement> getMetricElements(ChatQueryContext chatQueryContext,
            Long dataSetId) {
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        return semanticSchema.getMetrics(dataSetId);
    }

    protected Set<String> getDimensions(Long dataSetId, SemanticSchema semanticSchema) {
        Set<String> dimensions =
                semanticSchema.getDimensions(dataSetId).stream().flatMap(schemaElement -> {
                    Set<String> elements = new HashSet<>();
                    elements.add(schemaElement.getName());
                    if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                        elements.addAll(schemaElement.getAlias());
                    }
                    return elements.stream();
                }).collect(Collectors.toSet());
        dimensions.add(TimeDimensionEnum.DAY.getChName());
        return dimensions;
    }

    protected boolean containsPartitionDimensions(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        Long dataSetId = semanticParseInfo.getDataSetId();
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        DataSetSchema dataSetSchema = semanticSchema.getDataSetSchemaMap().get(dataSetId);
        return dataSetSchema.containsPartitionDimensions();
    }

    protected void removeDateIfExist(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        Set<String> removeFieldNames = new HashSet<>();
        removeFieldNames.addAll(TimeDimensionEnum.getChNameList());
        removeFieldNames.addAll(TimeDimensionEnum.getNameList());
        Map<String, String> fieldNameMap =
                getFieldNameMapFromDB(chatQueryContext, semanticParseInfo.getDataSetId());
        removeFieldNames.removeIf(fieldName -> fieldNameMap.containsKey(fieldName));
        if (!CollectionUtils.isEmpty(removeFieldNames)) {
            correctS2SQL = SqlRemoveHelper.removeWhereCondition(correctS2SQL, removeFieldNames);
            correctS2SQL = SqlRemoveHelper.removeSelect(correctS2SQL, removeFieldNames);
            correctS2SQL = SqlRemoveHelper.removeGroupBy(correctS2SQL, removeFieldNames);
        }
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
    }
}
