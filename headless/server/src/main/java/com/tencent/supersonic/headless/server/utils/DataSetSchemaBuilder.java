package com.tencent.supersonic.headless.server.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.RelatedSchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.SchemaValueMap;
import com.tencent.supersonic.headless.api.pojo.response.DataSetSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.TermResp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSetSchemaBuilder {

    public static DataSetSchema build(DataSetSchemaResp resp) {
        DataSetSchema dataSetSchema = new DataSetSchema();
        dataSetSchema.setQueryConfig(resp.getQueryConfig());
        SchemaElement dataSet = SchemaElement.builder()
                .dataSet(resp.getId())
                .dataSetName(resp.getName())
                .id(resp.getId())
                .name(resp.getName())
                .bizName(resp.getBizName())
                .type(SchemaElementType.DATASET)
                .build();
        dataSetSchema.setDataSet(dataSet);

        Set<SchemaElement> metrics = getMetrics(resp);
        dataSetSchema.getMetrics().addAll(metrics);

        Set<SchemaElement> metricTags = getMetricTags(resp);
        dataSetSchema.getTags().addAll(metricTags);

        Set<SchemaElement> dimensions = getDimensions(resp);
        dataSetSchema.getDimensions().addAll(dimensions);

        Set<SchemaElement> dimensionTags = getDimensionTags(resp);
        dataSetSchema.getTags().addAll(dimensionTags);

        Set<SchemaElement> dimensionValues = getDimensionValues(resp);
        dataSetSchema.getDimensionValues().addAll(dimensionValues);

        Set<SchemaElement> terms = getTerms(resp);
        dataSetSchema.getTerms().addAll(terms);

        SchemaElement entity = getEntity(resp);
        if (Objects.nonNull(entity)) {
            dataSetSchema.setEntity(entity);
        }
        return dataSetSchema;
    }

    private static Set<SchemaElement> getMetricTags(DataSetSchemaResp resp) {
        Set<SchemaElement> tags = new HashSet<>();
        for (MetricSchemaResp metric : resp.getMetrics()) {
            List<String> alias = SchemaItem.getAliasList(metric.getAlias());
            if (metric.getIsTag() == 1) {
                SchemaElement tagToAdd = SchemaElement.builder()
                        .dataSet(resp.getId())
                        .dataSetName(resp.getName())
                        .model(metric.getModelId())
                        .id(metric.getId())
                        .name(metric.getName())
                        .bizName(metric.getBizName())
                        .type(SchemaElementType.TAG)
                        .useCnt(metric.getUseCnt())
                        .alias(alias)
                        .defaultAgg(metric.getDefaultAgg())
                        .isTag(metric.getIsTag())
                        .description(metric.getDescription())
                        .build();
                tags.add(tagToAdd);
            }
        }
        return tags;
    }

    private static Set<SchemaElement> getDimensionTags(DataSetSchemaResp resp) {
        Set<SchemaElement> tags = new HashSet<>();
        for (DimSchemaResp dim : resp.getDimensions()) {
            List<String> alias = SchemaItem.getAliasList(dim.getAlias());
            List<DimValueMap> dimValueMaps = dim.getDimValueMaps();
            List<SchemaValueMap> schemaValueMaps = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dimValueMaps)) {
                for (DimValueMap dimValueMap : dimValueMaps) {
                    SchemaValueMap schemaValueMap = new SchemaValueMap();
                    BeanUtils.copyProperties(dimValueMap, schemaValueMap);
                    schemaValueMaps.add(schemaValueMap);
                }
            }
            if (dim.getIsTag() == 1) {
                SchemaElement tagToAdd = SchemaElement.builder()
                        .dataSet(resp.getId())
                        .dataSetName(resp.getName())
                        .model(dim.getModelId())
                        .id(dim.getId())
                        .name(dim.getName())
                        .bizName(dim.getBizName())
                        .type(SchemaElementType.TAG)
                        .useCnt(dim.getUseCnt())
                        .alias(alias)
                        .schemaValueMaps(schemaValueMaps)
                        .isTag(dim.getIsTag())
                        .description(dim.getDescription())
                        .build();
                tags.add(tagToAdd);
            }
        }
        return tags;
    }

    private static SchemaElement getEntity(DataSetSchemaResp resp) {
        DimSchemaResp dim = resp.getPrimaryKey();
        if (Objects.isNull(dim)) {
            return null;
        }
        return SchemaElement.builder()
                .dataSet(resp.getId())
                .model(dim.getModelId())
                .id(dim.getId())
                .name(dim.getName())
                .bizName(dim.getBizName())
                .type(SchemaElementType.ENTITY)
                .useCnt(dim.getUseCnt())
                .alias(dim.getEntityAlias())
                .build();
    }

    private static Set<SchemaElement> getDimensions(DataSetSchemaResp resp) {
        Set<SchemaElement> dimensions = new HashSet<>();
        for (DimSchemaResp dim : resp.getDimensions()) {
            List<String> alias = SchemaItem.getAliasList(dim.getAlias());
            List<DimValueMap> dimValueMaps = dim.getDimValueMaps();
            List<SchemaValueMap> schemaValueMaps = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dimValueMaps)) {
                for (DimValueMap dimValueMap : dimValueMaps) {
                    SchemaValueMap schemaValueMap = new SchemaValueMap();
                    BeanUtils.copyProperties(dimValueMap, schemaValueMap);
                    schemaValueMaps.add(schemaValueMap);
                }
            }
            SchemaElement dimToAdd = SchemaElement.builder()
                    .dataSet(resp.getId())
                    .dataSetName(resp.getName())
                    .model(dim.getModelId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.DIMENSION)
                    .useCnt(dim.getUseCnt())
                    .alias(alias)
                    .schemaValueMaps(schemaValueMaps)
                    .isTag(dim.getIsTag())
                    .description(dim.getDescription())
                    .build();
            dimensions.add(dimToAdd);
        }
        return dimensions;
    }

    private static Set<SchemaElement> getDimensionValues(DataSetSchemaResp resp) {
        Set<SchemaElement> dimensionValues = new HashSet<>();
        for (DimSchemaResp dim : resp.getDimensions()) {
            Set<String> dimValueAlias = new HashSet<>();
            List<DimValueMap> dimValueMaps = dim.getDimValueMaps();
            if (!CollectionUtils.isEmpty(dimValueMaps)) {
                for (DimValueMap dimValueMap : dimValueMaps) {
                    if (StringUtils.isNotEmpty(dimValueMap.getBizName())) {
                        dimValueAlias.add(dimValueMap.getBizName());
                    }
                    if (!CollectionUtils.isEmpty(dimValueMap.getAlias())) {
                        dimValueAlias.addAll(dimValueMap.getAlias());
                    }
                }
            }
            SchemaElement dimValueToAdd = SchemaElement.builder()
                    .dataSet(resp.getId())
                    .dataSetName(resp.getName())
                    .model(dim.getModelId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.VALUE)
                    .useCnt(dim.getUseCnt())
                    .alias(new ArrayList<>(Arrays.asList(dimValueAlias.toArray(new String[0]))))
                    .isTag(dim.getIsTag())
                    .description(dim.getDescription())
                    .build();
            dimensionValues.add(dimValueToAdd);
        }
        return dimensionValues;
    }

    private static Set<SchemaElement> getMetrics(DataSetSchemaResp resp) {
        Set<SchemaElement> metrics = new HashSet<>();
        for (MetricSchemaResp metric : resp.getMetrics()) {

            List<String> alias = SchemaItem.getAliasList(metric.getAlias());

            SchemaElement metricToAdd = SchemaElement.builder()
                    .dataSet(resp.getId())
                    .dataSetName(resp.getName())
                    .model(metric.getModelId())
                    .id(metric.getId())
                    .name(metric.getName())
                    .bizName(metric.getBizName())
                    .type(SchemaElementType.METRIC)
                    .useCnt(metric.getUseCnt())
                    .alias(alias)
                    .relatedSchemaElements(getRelateSchemaElement(metric))
                    .defaultAgg(metric.getDefaultAgg())
                    .dataFormatType(metric.getDataFormatType())
                    .isTag(metric.getIsTag())
                    .description(metric.getDescription())
                    .build();
            metrics.add(metricToAdd);

        }
        return metrics;
    }

    private static Set<SchemaElement> getTerms(DataSetSchemaResp resp) {
        Set<SchemaElement> terms = new HashSet<>();
        for (TermResp termResp : resp.getTermResps()) {
            List<String> alias = termResp.getAlias();
            SchemaElement metricToAdd = SchemaElement.builder()
                    .dataSet(resp.getId())
                    .dataSetName(resp.getName())
                    .model(-1L)
                    .id(termResp.getId())
                    .name(termResp.getName())
                    .bizName(termResp.getName())
                    .type(SchemaElementType.TERM)
                    .useCnt(0L)
                    .alias(alias)
                    .description(termResp.getDescription())
                    .build();
            terms.add(metricToAdd);

        }
        return terms;
    }

    private static List<RelatedSchemaElement> getRelateSchemaElement(MetricSchemaResp metricSchemaResp) {
        RelateDimension relateDimension = metricSchemaResp.getRelateDimension();
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            return Lists.newArrayList();
        }
        return relateDimension.getDrillDownDimensions().stream().map(dimension -> {
            RelatedSchemaElement relateSchemaElement = new RelatedSchemaElement();
            BeanUtils.copyProperties(dimension, relateSchemaElement);
            return relateSchemaElement;
        }).collect(Collectors.toList());
    }

}
