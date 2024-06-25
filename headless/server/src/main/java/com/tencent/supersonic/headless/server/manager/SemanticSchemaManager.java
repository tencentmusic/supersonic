package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.Field;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataType;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Identify;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.JoinRelation;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Materialization.TimePartType;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Measure;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.MetricTypeParams;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionTimeTypeParamsTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.FieldParamYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.IdentifyYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricParamYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.server.service.CatalogService;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class SemanticSchemaManager {

    private final CatalogService catalog;

    public SemanticSchemaManager(CatalogService catalog) {
        this.catalog = catalog;
    }

    public SemanticModel getSemanticModel(SemanticSchemaResp semanticSchemaResp) {
        SemanticModel semanticModel = new SemanticModel();
        semanticModel.setSchemaKey(semanticSchemaResp.getSchemaKey());
        Map<String, List<DimensionYamlTpl>> dimensionYamlTpls = new HashMap<>();
        List<DataModelYamlTpl> dataModelYamlTpls = new ArrayList<>();
        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        Map<Long, String> modelIdName = new HashMap<>();
        catalog.getSchemaYamlTpl(semanticSchemaResp, dimensionYamlTpls,
                dataModelYamlTpls, metricYamlTpls, modelIdName);
        DatabaseResp databaseResp = semanticSchemaResp.getDatabaseResp();
        semanticModel.setDatabase(DatabaseConverter.convert(databaseResp));
        if (!CollectionUtils.isEmpty(semanticSchemaResp.getModelRelas())) {
            semanticModel.setJoinRelations(getJoinRelation(semanticSchemaResp.getModelRelas(), modelIdName));
        }
        if (!dataModelYamlTpls.isEmpty()) {
            Map<String, DataSource> dataSourceMap = dataModelYamlTpls.stream().map(SemanticSchemaManager::getDatasource)
                    .collect(Collectors.toMap(DataSource::getName, item -> item, (k1, k2) -> k1));
            semanticModel.setDatasourceMap(dataSourceMap);
        }
        if (!dimensionYamlTpls.isEmpty()) {
            Map<String, List<Dimension>> dimensionMap = new HashMap<>();
            for (Map.Entry<String, List<DimensionYamlTpl>> entry : dimensionYamlTpls.entrySet()) {
                dimensionMap.put(entry.getKey(), getDimensions(entry.getValue()));
            }
            semanticModel.setDimensionMap(dimensionMap);
        }
        if (!metricYamlTpls.isEmpty()) {
            semanticModel.setMetrics(getMetrics(metricYamlTpls));
        }
        return semanticModel;
    }

    public SemanticModel getTagSemanticModel(SemanticSchemaResp semanticSchemaResp) throws Exception {
        if (CollectionUtils.isEmpty(semanticSchemaResp.getTags())) {
            throw new Exception("semanticSchemaResp tag is empty");
        }
        SemanticModel semanticModel = getSemanticModel(semanticSchemaResp);
        //Map<String, List<Dimension>> dimensions = new HashMap<>();
        Map<Long, List<TagResp>> tagMap = new HashMap<>();
        for (TagResp tagResp : semanticSchemaResp.getTags()) {
            if (!tagMap.containsKey(tagResp.getModelId())) {
                tagMap.put(tagResp.getModelId(), new ArrayList<>());
            }
            tagMap.get(tagResp.getModelId()).add(tagResp);
        }
        if (Objects.nonNull(semanticModel.getDatasourceMap()) && !semanticModel.getDatasourceMap().isEmpty()) {
            for (Map.Entry<String, DataSource> entry : semanticModel.getDatasourceMap().entrySet()) {
                List<Dimension> modelDimensions = new ArrayList<>();
                if (!semanticModel.getDimensionMap().containsKey(entry.getKey())) {
                    semanticModel.getDimensionMap().put(entry.getKey(), modelDimensions);
                } else {
                    modelDimensions = semanticModel.getDimensionMap().get(entry.getKey());
                }
                if (tagMap.containsKey(entry.getValue().getId())) {
                    for (TagResp tagResp : tagMap.get(entry.getValue().getId())) {
                        addTagModel(tagResp, modelDimensions, semanticModel.getMetrics());
                    }
                }
            }
        }

        return semanticModel;
    }

    private void addTagModel(TagResp tagResp, List<Dimension> modelDimensions, List<Metric> modelMetrics)
            throws Exception {
        TagDefineType tagDefineType = TagDefineType.valueOf(tagResp.getTagDefineType());
        switch (tagDefineType) {
            case FIELD:
            case DIMENSION:
                if (TagDefineType.DIMENSION.equals(tagResp.getTagDefineType())) {
                    Optional<Dimension> modelDimension = modelDimensions.stream()
                            // .filter(d -> d.getBizName().equals(tagResp.getExpr()))
                            .findFirst();
                    if (modelDimension.isPresent()) {
                        modelDimension.get().setName(tagResp.getBizName());
                        return;
                    }
                }
                Dimension dimension = Dimension.builder().build();
                dimension.setType("");
                //  dimension.setExpr(tagResp.getExpr());
                dimension.setName(tagResp.getBizName());
                dimension.setOwners("");
                dimension.setBizName(tagResp.getBizName());
                if (Objects.isNull(dimension.getDataType())) {
                    dimension.setDataType(DataType.UNKNOWN);
                }

                DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams();
                dimension.setDimensionTimeTypeParams(dimensionTimeTypeParams);
                modelDimensions.add(dimension);
                return;
            case METRIC:
                Optional<Metric> modelMetric = modelMetrics.stream()
                        // .filter(m -> m.getName().equalsIgnoreCase(tagResp.getExpr()))
                        .findFirst();
                if (modelMetric.isPresent()) {
                    modelMetric.get().setName(tagResp.getBizName());
                } else {
                    throw new Exception(String.format("tag [{}] cant find the metric", tagResp.getBizName()));
                }
                return;
            default:
        }
    }

    public static List<Metric> getMetrics(final List<MetricYamlTpl> t) {
        return getMetricsByMetricYamlTpl(t);
    }

    public static List<Dimension> getDimensions(final List<DimensionYamlTpl> t) {
        return getDimension(t);
    }

    public static DataSource getDatasource(final DataModelYamlTpl d) {
        DataSource datasource = DataSource.builder().id(d.getId()).sourceId(d.getSourceId())
                .type(d.getType()).sqlQuery(d.getSqlQuery()).name(d.getName()).tableQuery(d.getTableQuery())
                .identifiers(getIdentify(d.getIdentifiers())).measures(getMeasureParams(d.getMeasures()))
                .dimensions(getDimensions(d.getDimensions())).build();
        datasource.setAggTime(getDataSourceAggTime(datasource.getDimensions()));
        if (Objects.nonNull(d.getModelSourceTypeEnum())) {
            datasource.setTimePartType(TimePartType.of(d.getModelSourceTypeEnum().name()));
        }
        if (Objects.nonNull(d.getFields()) && !CollectionUtils.isEmpty(d.getFields())) {
            Set<String> measures = datasource.getMeasures().stream().map(mm -> mm.getName())
                    .collect(Collectors.toSet());
            for (Field f : d.getFields()) {
                if (!measures.contains(f.getFieldName())) {
                    datasource.getMeasures()
                            .add(Measure.builder().expr(f.getFieldName()).name(f.getFieldName()).agg("").build());
                }
            }
        }
        return datasource;
    }

    private static String getDataSourceAggTime(List<Dimension> dimensions) {
        Optional<Dimension> timeDimension = dimensions.stream()
                .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType())).findFirst();
        if (timeDimension.isPresent() && Objects.nonNull(timeDimension.get().getDimensionTimeTypeParams())) {
            return timeDimension.get().getDimensionTimeTypeParams().getTimeGranularity();
        }
        return Constants.DIMENSION_TYPE_TIME_GRANULARITY_NONE;
    }

    private static List<Metric> getMetricsByMetricYamlTpl(List<MetricYamlTpl> metricYamlTpls) {
        List<Metric> metrics = new ArrayList<>();
        for (MetricYamlTpl metricYamlTpl : metricYamlTpls) {
            Metric metric = new Metric();
            metric.setMetricTypeParams(getMetricTypeParams(metricYamlTpl.getTypeParams()));
            metric.setOwners(metricYamlTpl.getOwners());
            metric.setType(metricYamlTpl.getType());
            metric.setName(metricYamlTpl.getName());
            metrics.add(metric);
        }
        return metrics;
    }

    private static MetricTypeParams getMetricTypeParams(MetricTypeParamsYamlTpl metricTypeParamsYamlTpl) {
        MetricTypeParams metricTypeParams = new MetricTypeParams();
        metricTypeParams.setExpr(metricTypeParamsYamlTpl.getExpr());
        metricTypeParams.setFieldMetric(false);
        if (!CollectionUtils.isEmpty(metricTypeParamsYamlTpl.getMeasures())) {
            metricTypeParams.setMeasures(getMeasureParams(metricTypeParamsYamlTpl.getMeasures()));
        }
        if (!CollectionUtils.isEmpty(metricTypeParamsYamlTpl.getMetrics())) {
            metricTypeParams.setMeasures(getMetricParams(metricTypeParamsYamlTpl.getMetrics()));
            metricTypeParams.setExpr(metricTypeParams.getMeasures().get(0).getExpr());
            metricTypeParams.setFieldMetric(true);
        }
        if (!CollectionUtils.isEmpty(metricTypeParamsYamlTpl.getFields())) {
            metricTypeParams.setMeasures(getFieldParams(metricTypeParamsYamlTpl.getFields()));
            metricTypeParams.setExpr(metricTypeParams.getMeasures().get(0).getExpr());
            metricTypeParams.setFieldMetric(true);
        }

        return metricTypeParams;
    }

    private static List<Measure> getFieldParams(List<FieldParamYamlTpl> fieldParamYamlTpls) {
        List<Measure> measures = new ArrayList<>();
        for (FieldParamYamlTpl fieldParamYamlTpl : fieldParamYamlTpls) {
            Measure measure = new Measure();
            measure.setName(fieldParamYamlTpl.getFieldName());
            measure.setExpr(fieldParamYamlTpl.getFieldName());
            measures.add(measure);
        }
        return measures;
    }

    private static List<Measure> getMetricParams(List<MetricParamYamlTpl> metricParamYamlTpls) {
        List<Measure> measures = new ArrayList<>();
        for (MetricParamYamlTpl metricParamYamlTpl : metricParamYamlTpls) {
            Measure measure = new Measure();
            measure.setName(metricParamYamlTpl.getBizName());
            measure.setExpr(metricParamYamlTpl.getBizName());
            measures.add(measure);
        }
        return measures;
    }

    private static List<Measure> getMeasureParams(List<MeasureYamlTpl> measureYamlTpls) {
        List<Measure> measures = new ArrayList<>();
        for (MeasureYamlTpl measureYamlTpl : measureYamlTpls) {
            Measure measure = new Measure();
            measure.setCreateMetric(measureYamlTpl.getCreateMetric());
            measure.setExpr(measureYamlTpl.getExpr());
            measure.setAgg(measureYamlTpl.getAgg());
            measure.setName(measureYamlTpl.getName());
            measure.setAlias(measureYamlTpl.getAlias());
            measure.setConstraint(measureYamlTpl.getConstraint());
            measures.add(measure);
        }
        return measures;
    }

    private static List<Dimension> getDimension(List<DimensionYamlTpl> dimensionYamlTpls) {
        List<Dimension> dimensions = new ArrayList<>();
        for (DimensionYamlTpl dimensionYamlTpl : dimensionYamlTpls) {
            Dimension dimension = Dimension.builder().build();
            dimension.setType(dimensionYamlTpl.getType());
            dimension.setExpr(dimensionYamlTpl.getExpr());
            dimension.setName(dimensionYamlTpl.getName());
            dimension.setOwners(dimensionYamlTpl.getOwners());
            dimension.setBizName(dimensionYamlTpl.getBizName());
            dimension.setDefaultValues(dimensionYamlTpl.getDefaultValues());
            if (Objects.nonNull(dimensionYamlTpl.getDataType())) {
                dimension.setDataType(DataType.of(dimensionYamlTpl.getDataType().getType()));
            }
            if (Objects.isNull(dimension.getDataType())) {
                dimension.setDataType(DataType.UNKNOWN);
            }
            if (Objects.nonNull(dimensionYamlTpl.getExt())) {
                dimension.setExt(dimensionYamlTpl.getExt());
            }
            dimension.setDimensionTimeTypeParams(getDimensionTimeTypeParams(dimensionYamlTpl.getTypeParams()));
            dimensions.add(dimension);
        }
        return dimensions;
    }

    private static DimensionTimeTypeParams getDimensionTimeTypeParams(
            DimensionTimeTypeParamsTpl dimensionTimeTypeParamsTpl) {
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams();
        if (dimensionTimeTypeParamsTpl != null) {
            dimensionTimeTypeParams.setTimeGranularity(dimensionTimeTypeParamsTpl.getTimeGranularity());
            dimensionTimeTypeParams.setIsPrimary(dimensionTimeTypeParamsTpl.getIsPrimary());
        }
        return dimensionTimeTypeParams;
    }

    private static List<Identify> getIdentify(List<IdentifyYamlTpl> identifyYamlTpls) {
        List<Identify> identifies = new ArrayList<>();
        for (IdentifyYamlTpl identifyYamlTpl : identifyYamlTpls) {
            Identify identify = new Identify();
            identify.setType(identifyYamlTpl.getType());
            identify.setName(identifyYamlTpl.getName());
            identifies.add(identify);
        }
        return identifies;
    }

    private static List<JoinRelation> getJoinRelation(List<ModelRela> modelRelas, Map<Long, String> modelIdName) {
        List<JoinRelation> joinRelations = new ArrayList<>();
        modelRelas.stream().forEach(r -> {
            if (modelIdName.containsKey(r.getFromModelId()) && modelIdName.containsKey(r.getToModelId())) {
                JoinRelation joinRelation = JoinRelation.builder().left(modelIdName.get(r.getFromModelId()))
                        .right(modelIdName.get(r.getToModelId())).joinType(r.getJoinType()).build();
                List<Triple<String, String, String>> conditions = new ArrayList<>();
                r.getJoinConditions().stream().forEach(rr -> {
                    if (FilterOperatorEnum.isValueCompare(rr.getOperator())) {
                        conditions.add(
                                Triple.of(rr.getLeftField(), rr.getOperator().getValue(), rr.getRightField()));
                    }
                });
                joinRelation.setId(r.getId());
                joinRelation.setJoinCondition(conditions);
                joinRelations.add(joinRelation);
            }
        });
        return joinRelations;
    }

    public static void update(SemanticSchema schema, List<Metric> metric) throws Exception {
        if (schema != null) {
            updateMetric(metric, schema.getMetrics());
        }
    }

    public static void update(SemanticSchema schema, DataSource datasourceYamlTpl) throws Exception {
        if (schema != null) {
            String dataSourceName = datasourceYamlTpl.getName();
            Optional<Entry<String, DataSource>> datasourceYamlTplMap = schema.getDatasource().entrySet().stream()
                    .filter(t -> t.getKey().equalsIgnoreCase(dataSourceName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                datasourceYamlTplMap.get().setValue(datasourceYamlTpl);
            } else {
                schema.getDatasource().put(dataSourceName, datasourceYamlTpl);
            }
        }
    }

    public static void update(SemanticSchema schema, String datasourceBizName, List<Dimension> dimensionYamlTpls)
            throws Exception {
        if (schema != null) {
            Optional<Map.Entry<String, List<Dimension>>> datasourceYamlTplMap = schema.getDimension().entrySet()
                    .stream().filter(t -> t.getKey().equalsIgnoreCase(datasourceBizName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                updateDimension(dimensionYamlTpls, datasourceYamlTplMap.get().getValue());
            } else {
                List<Dimension> dimensions = new ArrayList<>();
                updateDimension(dimensionYamlTpls, dimensions);
                schema.getDimension().put(datasourceBizName, dimensions);
            }
        }
    }

    private static void updateDimension(List<Dimension> dimensionYamlTpls, List<Dimension> dimensions) {
        if (CollectionUtils.isEmpty(dimensionYamlTpls)) {
            return;
        }
        Set<String> toAdd = dimensionYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
        Iterator<Dimension> iterator = dimensions.iterator();
        while (iterator.hasNext()) {
            Dimension cur = iterator.next();
            if (toAdd.contains(cur.getName())) {
                iterator.remove();
            }
        }
        dimensions.addAll(dimensionYamlTpls);
    }

    private static void updateMetric(List<Metric> metricYamlTpls, List<Metric> metrics) {
        if (CollectionUtils.isEmpty(metricYamlTpls)) {
            return;
        }
        Set<String> toAdd = metricYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
        Iterator<Metric> iterator = metrics.iterator();
        while (iterator.hasNext()) {
            Metric cur = iterator.next();
            if (toAdd.contains(cur.getName())) {
                iterator.remove();
            }
        }
        metrics.addAll(metricYamlTpls);
    }

}