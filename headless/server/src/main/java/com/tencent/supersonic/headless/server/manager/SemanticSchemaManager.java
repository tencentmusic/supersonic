package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.*;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization.TimePartType;
import com.tencent.supersonic.headless.server.pojo.yaml.*;
import com.tencent.supersonic.headless.server.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SemanticSchemaManager {

    private final SchemaService schemaService;

    public SemanticSchemaManager(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public Ontology buildOntology(SemanticSchemaResp semanticSchemaResp) {
        Ontology ontology = new Ontology();
        Map<String, List<DimensionYamlTpl>> dimensionYamlTpls = new HashMap<>();
        List<DataModelYamlTpl> dataModelYamlTpls = new ArrayList<>();
        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        Map<Long, String> modelIdName = new HashMap<>();
        schemaService.getSchemaYamlTpl(semanticSchemaResp, dimensionYamlTpls, dataModelYamlTpls,
                metricYamlTpls, modelIdName);
        DatabaseResp databaseResp = semanticSchemaResp.getDatabaseResp();
        ontology.setDatabase(databaseResp);
        if (!CollectionUtils.isEmpty(semanticSchemaResp.getModelRelas())) {
            ontology.setJoinRelations(
                    getJoinRelation(semanticSchemaResp.getModelRelas(), modelIdName));
        }
        if (!dataModelYamlTpls.isEmpty()) {
            Map<String, DataModel> dataModelMap =
                    dataModelYamlTpls.stream().map(SemanticSchemaManager::getDataModel).collect(
                            Collectors.toMap(DataModel::getName, item -> item, (k1, k2) -> k1));
            ontology.setDataModelMap(dataModelMap);
        }
        if (!dimensionYamlTpls.isEmpty()) {
            Map<String, List<Dimension>> dimensionMap = new HashMap<>();
            for (Map.Entry<String, List<DimensionYamlTpl>> entry : dimensionYamlTpls.entrySet()) {
                dimensionMap.put(entry.getKey(), getDimensions(entry.getValue()));
            }
            ontology.setDimensionMap(dimensionMap);
        }
        if (!metricYamlTpls.isEmpty()) {
            ontology.setMetrics(getMetrics(metricYamlTpls));
        }
        return ontology;
    }

    public static List<Metric> getMetrics(final List<MetricYamlTpl> t) {
        return getMetricsByMetricYamlTpl(t);
    }

    public static List<Dimension> getDimensions(final List<DimensionYamlTpl> t) {
        return getDimension(t);
    }

    public static DataModel getDataModel(final DataModelYamlTpl d) {
        DataModel dataModel = DataModel.builder().id(d.getId()).modelId(d.getSourceId())
                .type(d.getType()).sqlQuery(d.getSqlQuery()).name(d.getName())
                .tableQuery(d.getTableQuery()).identifiers(getIdentify(d.getIdentifiers()))
                .measures(getMeasureParams(d.getMeasures()))
                .dimensions(getDimensions(d.getDimensions())).build();
        dataModel.setAggTime(getDataModelAggTime(dataModel.getDimensions()));
        if (Objects.nonNull(d.getModelSourceTypeEnum())) {
            dataModel.setTimePartType(TimePartType.of(d.getModelSourceTypeEnum().name()));
        }
        return dataModel;
    }

    private static String getDataModelAggTime(List<Dimension> dimensions) {
        Optional<Dimension> timeDimension = dimensions.stream()
                .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                .findFirst();
        if (timeDimension.isPresent()
                && Objects.nonNull(timeDimension.get().getDimensionTimeTypeParams())) {
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

    private static MetricTypeParams getMetricTypeParams(
            MetricTypeParamsYamlTpl metricTypeParamsYamlTpl) {
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
            dimension.setDimensionTimeTypeParams(dimensionYamlTpl.getTypeParams());
            dimensions.add(dimension);
        }
        return dimensions;
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

    private static List<JoinRelation> getJoinRelation(List<ModelRela> modelRelas,
            Map<Long, String> modelIdName) {
        List<JoinRelation> joinRelations = new ArrayList<>();
        modelRelas.stream().forEach(r -> {
            if (modelIdName.containsKey(r.getFromModelId())
                    && modelIdName.containsKey(r.getToModelId())) {
                JoinRelation joinRelation = JoinRelation.builder()
                        .left(modelIdName.get(r.getFromModelId()))
                        .right(modelIdName.get(r.getToModelId())).joinType(r.getJoinType()).build();
                List<Triple<String, String, String>> conditions = new ArrayList<>();
                r.getJoinConditions().stream().forEach(rr -> {
                    if (FilterOperatorEnum.isValueCompare(rr.getOperator())) {
                        conditions.add(Triple.of(rr.getLeftField(), rr.getOperator().getValue(),
                                rr.getRightField()));
                    }
                });
                joinRelation.setId(r.getId());
                joinRelation.setJoinCondition(conditions);
                joinRelations.add(joinRelation);
            }
        });
        return joinRelations;
    }

    public static void update(S2CalciteSchema schema, List<Metric> metric) throws Exception {
        if (schema != null) {
            updateMetric(metric, schema.getMetrics());
        }
    }

    public static void update(S2CalciteSchema schema, DataModel datasourceYamlTpl)
            throws Exception {
        if (schema != null) {
            String dataSourceName = datasourceYamlTpl.getName();
            Optional<Entry<String, DataModel>> datasourceYamlTplMap =
                    schema.getDataModels().entrySet().stream()
                            .filter(t -> t.getKey().equalsIgnoreCase(dataSourceName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                datasourceYamlTplMap.get().setValue(datasourceYamlTpl);
            } else {
                schema.getDataModels().put(dataSourceName, datasourceYamlTpl);
            }
        }
    }

    public static void update(S2CalciteSchema schema, String datasourceBizName,
            List<Dimension> dimensionYamlTpls) throws Exception {
        if (schema != null) {
            Optional<Map.Entry<String, List<Dimension>>> datasourceYamlTplMap = schema
                    .getDimensions().entrySet().stream()
                    .filter(t -> t.getKey().equalsIgnoreCase(datasourceBizName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                updateDimension(dimensionYamlTpls, datasourceYamlTplMap.get().getValue());
            } else {
                List<Dimension> dimensions = new ArrayList<>();
                updateDimension(dimensionYamlTpls, dimensions);
                schema.getDimensions().put(datasourceBizName, dimensions);
            }
        }
    }

    private static void updateDimension(List<Dimension> dimensionYamlTpls,
            List<Dimension> dimensions) {
        if (CollectionUtils.isEmpty(dimensionYamlTpls)) {
            return;
        }
        Set<String> toAdd =
                dimensionYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
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
        Set<String> toAdd =
                metricYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
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
