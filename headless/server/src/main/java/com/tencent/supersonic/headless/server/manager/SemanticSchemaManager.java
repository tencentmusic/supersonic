package com.tencent.supersonic.headless.server.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.DataModel;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.*;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization.TimePartType;
import com.tencent.supersonic.headless.server.pojo.yaml.*;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
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
        ontology.setMetrics(semanticSchemaResp.getMetrics());
        Map<String, List<DimSchemaResp>> model2Dimensions = Maps.newHashMap();
        semanticSchemaResp.getDimensions().forEach(dim -> {
            if (!model2Dimensions.containsKey(dim.getModelBizName())) {
                model2Dimensions.put(dim.getModelBizName(), Lists.newArrayList());
            }
            model2Dimensions.get(dim.getModelBizName()).add(dim);
        });
        ontology.setDimensionMap(model2Dimensions);

        Map<String, List<DimensionYamlTpl>> dimensionYamlTpls = new HashMap<>();
        List<DataModelYamlTpl> dataModelYamlTpls = new ArrayList<>();
        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        Map<Long, String> modelIdName = new HashMap<>();
        schemaService.getSchemaYamlTpl(semanticSchemaResp, dimensionYamlTpls, dataModelYamlTpls,
                metricYamlTpls, modelIdName);
        DatabaseResp databaseResp = semanticSchemaResp.getDatabaseResp();
        ontology.setDatabase(DatabaseConverter.convert(databaseResp));
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

        return ontology;
    }

    public static List<MetricSchemaResp> getMetrics(final List<MetricYamlTpl> t) {
        return getMetricsByMetricYamlTpl(t);
    }

    public static List<DimSchemaResp> getDimensions(final List<DimensionYamlTpl> t) {
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

    private static String getDataModelAggTime(List<DimSchemaResp> dimensions) {
        Optional<DimSchemaResp> timeDimension =
                dimensions.stream().filter(DimSchemaResp::isTimeDimension).findFirst();
        if (timeDimension.isPresent() && Objects.nonNull(timeDimension.get().getTypeParams())) {
            return timeDimension.get().getTypeParams().getTimeGranularity();
        }
        return Constants.DIMENSION_TYPE_TIME_GRANULARITY_NONE;
    }

    private static List<MetricSchemaResp> getMetricsByMetricYamlTpl(
            List<MetricYamlTpl> metricYamlTpls) {
        List<MetricSchemaResp> metrics = new ArrayList<>();
        for (MetricYamlTpl metricYamlTpl : metricYamlTpls) {
            MetricSchemaResp metric = new MetricSchemaResp();
            fillMetricTypeParams(metric, metricYamlTpl.getTypeParams());
            metric.setType(metricYamlTpl.getType());
            metric.setName(metricYamlTpl.getName());
            metrics.add(metric);
        }
        return metrics;
    }

    private static void fillMetricTypeParams(MetricSchemaResp metric,
            MetricTypeParamsYamlTpl metricTypeParamsYamlTpl) {
        if (!CollectionUtils.isEmpty(metricTypeParamsYamlTpl.getMeasures())) {
            MetricDefineByMeasureParams params = new MetricDefineByMeasureParams();
            params.setMeasures(getMeasureParams(metricTypeParamsYamlTpl.getMeasures()));
            metric.setMetricDefinition(MetricDefineType.MEASURE, params);
        } else if (!CollectionUtils.isEmpty(metricTypeParamsYamlTpl.getMetrics())) {
            MetricDefineByMetricParams params = new MetricDefineByMetricParams();
            params.setMetrics(getMetricParams(metricTypeParamsYamlTpl.getMetrics()));
            params.setExpr(metricTypeParamsYamlTpl.getExpr());
            metric.setMetricDefinition(MetricDefineType.METRIC, params);
        } else if (!CollectionUtils.isEmpty(metricTypeParamsYamlTpl.getFields())) {
            MetricDefineByFieldParams params = new MetricDefineByFieldParams();
            params.setExpr(metricTypeParamsYamlTpl.getExpr());
            params.setFields(getFieldParams(metricTypeParamsYamlTpl.getFields()));
            metric.setMetricDefinition(MetricDefineType.FIELD, params);
        }
    }

    private static List<FieldParam> getFieldParams(List<FieldParamYamlTpl> fieldParamYamlTpls) {
        List<FieldParam> fields = new ArrayList<>();
        for (FieldParamYamlTpl fieldParamYamlTpl : fieldParamYamlTpls) {
            FieldParam field = new FieldParam();
            field.setFieldName(fieldParamYamlTpl.getFieldName());
            fields.add(field);
        }
        return fields;
    }

    private static List<MetricParam> getMetricParams(List<MetricParamYamlTpl> metricParamYamlTpls) {
        List<MetricParam> metrics = new ArrayList<>();
        for (MetricParamYamlTpl metricParamYamlTpl : metricParamYamlTpls) {
            MetricParam metric = new MetricParam();
            metric.setBizName(metricParamYamlTpl.getBizName());
            metric.setId(metricParamYamlTpl.getId());
            metrics.add(metric);
        }
        return metrics;
    }

    private static List<Measure> getMeasureParams(List<MeasureYamlTpl> measureYamlTpls) {
        List<Measure> measures = new ArrayList<>();
        for (MeasureYamlTpl measureYamlTpl : measureYamlTpls) {
            Measure measure = new Measure();
            measure.setExpr(measureYamlTpl.getExpr());
            measure.setAgg(measureYamlTpl.getAgg());
            measure.setName(measureYamlTpl.getName());
            measure.setAlias(measureYamlTpl.getAlias());
            measure.setConstraint(measureYamlTpl.getConstraint());
            measures.add(measure);
        }
        return measures;
    }

    private static List<DimSchemaResp> getDimension(List<DimensionYamlTpl> dimensionYamlTpls) {
        List<DimSchemaResp> dimensions = new ArrayList<>();
        for (DimensionYamlTpl dimensionYamlTpl : dimensionYamlTpls) {
            DimSchemaResp dimension = new DimSchemaResp();
            // dimension.setType(dimensionYamlTpl.getType());
            dimension.setExpr(dimensionYamlTpl.getExpr());
            dimension.setName(dimensionYamlTpl.getName());
            dimension.setBizName(dimensionYamlTpl.getBizName());
            dimension.setDefaultValues(dimensionYamlTpl.getDefaultValues());
            if (Objects.nonNull(dimensionYamlTpl.getDataType())) {
                dimension.setDataType(dimensionYamlTpl.getDataType());
            }
            if (Objects.isNull(dimension.getDataType())) {
                dimension.setDataType(DataTypeEnums.UNKNOWN);
            }
            if (Objects.nonNull(dimensionYamlTpl.getExt())) {
                dimension.setExt(dimensionYamlTpl.getExt());
            }
            dimension.setTypeParams(dimensionYamlTpl.getTypeParams());
            dimensions.add(dimension);
        }
        return dimensions;
    }

    private static DimensionTimeTypeParams getDimensionTimeTypeParams(
            DimensionTimeTypeParams dimensionTimeTypeParamsTpl) {
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams();
        if (dimensionTimeTypeParamsTpl != null) {
            dimensionTimeTypeParams
                    .setTimeGranularity(dimensionTimeTypeParamsTpl.getTimeGranularity());
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

    public static void update(S2CalciteSchema schema, List<MetricSchemaResp> metric)
            throws Exception {
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
            List<DimSchemaResp> dimensionYamlTpls) throws Exception {
        if (schema != null) {
            Optional<Map.Entry<String, List<DimSchemaResp>>> datasourceYamlTplMap = schema
                    .getDimensions().entrySet().stream()
                    .filter(t -> t.getKey().equalsIgnoreCase(datasourceBizName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                updateDimension(dimensionYamlTpls, datasourceYamlTplMap.get().getValue());
            } else {
                List<DimSchemaResp> dimensions = new ArrayList<>();
                updateDimension(dimensionYamlTpls, dimensions);
                schema.getDimensions().put(datasourceBizName, dimensions);
            }
        }
    }

    private static void updateDimension(List<DimSchemaResp> dimensionYamlTpls,
            List<DimSchemaResp> dimensions) {
        if (CollectionUtils.isEmpty(dimensionYamlTpls)) {
            return;
        }
        Set<String> toAdd =
                dimensionYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
        Iterator<DimSchemaResp> iterator = dimensions.iterator();
        while (iterator.hasNext()) {
            DimSchemaResp cur = iterator.next();
            if (toAdd.contains(cur.getName())) {
                iterator.remove();
            }
        }
        dimensions.addAll(dimensionYamlTpls);
    }

    private static void updateMetric(List<MetricSchemaResp> metricYamlTpls,
            List<MetricSchemaResp> metrics) {
        if (CollectionUtils.isEmpty(metricYamlTpls)) {
            return;
        }
        Set<String> toAdd =
                metricYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
        Iterator<MetricSchemaResp> iterator = metrics.iterator();
        while (iterator.hasNext()) {
            MetricSchemaResp cur = iterator.next();
            if (toAdd.contains(cur.getName())) {
                iterator.remove();
            }
        }
        metrics.addAll(metricYamlTpls);
    }
}
