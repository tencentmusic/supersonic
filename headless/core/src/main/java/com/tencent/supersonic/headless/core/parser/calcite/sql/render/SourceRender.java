package com.tencent.supersonic.headless.core.parser.calcite.sql.render;


import static com.tencent.supersonic.headless.core.parser.calcite.s2sql.Constants.DIMENSION_DELIMITER;

import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Identify;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Materialization;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Measure;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.parser.calcite.sql.Renderer;
import com.tencent.supersonic.headless.core.parser.calcite.sql.TableView;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.DataSourceNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.DimensionNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.FilterNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.IdentifyNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.MetricNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.SemanticNode;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

/**
 * process the table dataSet from the defined data model schema
 */
@Slf4j
public class SourceRender extends Renderer {

    public static TableView renderOne(String alias, List<String> fieldWheres,
            List<String> reqMetrics, List<String> reqDimensions,
            String queryWhere, DataSource datasource, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {

        TableView dataSet = new TableView();
        TableView output = new TableView();
        List<String> queryMetrics = new ArrayList<>(reqMetrics);
        List<String> queryDimensions = new ArrayList<>(reqDimensions);
        List<String> fieldWhere = new ArrayList<>(fieldWheres);
        Map<String, String> extendFields = new HashMap<>();
        if (!fieldWhere.isEmpty()) {
            Set<String> dimensions = new HashSet<>();
            Set<String> metrics = new HashSet<>();
            whereDimMetric(fieldWhere, queryMetrics, queryDimensions, datasource, schema, dimensions, metrics);
            queryMetrics.addAll(metrics);
            queryMetrics = uniqList(queryMetrics);
            queryDimensions.addAll(dimensions);
            queryDimensions = uniqList(queryDimensions);
            mergeWhere(fieldWhere, dataSet, output, queryMetrics, queryDimensions, extendFields,
                    datasource, scope,
                    schema, nonAgg);
        }
        addTimeDimension(datasource, queryDimensions);
        for (String metric : queryMetrics) {
            MetricNode metricNode = buildMetricNode(metric, datasource, scope, schema, nonAgg, alias);
            if (!metricNode.getAggNode().isEmpty()) {
                metricNode.getAggNode().entrySet().stream().forEach(m -> output.getMeasure().add(m.getValue()));
            }
            if (metricNode.getNonAggNode() != null) {
                metricNode.getNonAggNode().entrySet().stream().forEach(m -> dataSet.getMeasure().add(m.getValue()));
            }
            if (metricNode.getMeasureFilter() != null) {
                metricNode.getMeasureFilter().entrySet().stream().forEach(m -> dataSet.getFilter().add(m.getValue()));
            }
        }
        for (String dimension : queryDimensions) {
            if (dimension.contains(Constants.DIMENSION_IDENTIFY) && queryDimensions.contains(
                    dimension.split(Constants.DIMENSION_IDENTIFY)[1])) {
                continue;
            }
            buildDimension(dimension.contains(Constants.DIMENSION_IDENTIFY) ? dimension : "",
                    dimension.contains(Constants.DIMENSION_IDENTIFY) ? dimension.split(Constants.DIMENSION_IDENTIFY)[1]
                            : dimension, datasource, schema, nonAgg, extendFields, dataSet, output,
                    scope);
        }

        SqlNode tableNode = DataSourceNode.buildExtend(datasource, extendFields, scope);
        dataSet.setTable(tableNode);
        output.setTable(SemanticNode.buildAs(
                Constants.DATASOURCE_TABLE_OUT_PREFIX + datasource.getName() + "_" + UUID.randomUUID().toString()
                        .substring(32), dataSet.build()));
        return output;
    }

    private static void buildDimension(String alias, String dimension, DataSource datasource, SemanticSchema schema,
            boolean nonAgg, Map<String, String> extendFields, TableView dataSet, TableView output,
            SqlValidatorScope scope)
            throws Exception {
        List<Dimension> dimensionList = schema.getDimension().get(datasource.getName());
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        boolean isAdd = false;
        if (!CollectionUtils.isEmpty(dimensionList)) {
            for (Dimension dim : dimensionList) {
                if (!dim.getName().equalsIgnoreCase(dimension)) {
                    continue;
                }
                dataSet.getMeasure().add(DimensionNode.buildArray(dim, scope, engineType));
                addExtendFields(dim, extendFields);
                if (nonAgg) {
                    output.getMeasure().add(DimensionNode.buildName(dim, scope, engineType));
                    isAdd = true;
                    continue;
                }

                if ("".equals(alias)) {
                    output.getDimension().add(DimensionNode.buildName(dim, scope, engineType));
                } else {
                    output.getDimension().add(DimensionNode.buildNameAs(alias, dim, scope, engineType));
                }
                isAdd = true;
                break;
            }
        }
        if (!isAdd) {
            Optional<Identify> identify = datasource.getIdentifiers().stream()
                    .filter(i -> i.getName().equalsIgnoreCase(dimension)).findFirst();
            if (identify.isPresent()) {
                if (nonAgg) {
                    dataSet.getMeasure().add(SemanticNode.parse(identify.get().getName(), scope, engineType));
                    output.getMeasure().add(SemanticNode.parse(identify.get().getName(), scope, engineType));
                } else {
                    dataSet.getMeasure().add(SemanticNode.parse(identify.get().getName(), scope, engineType));
                    output.getDimension().add(SemanticNode.parse(identify.get().getName(), scope, engineType));
                }
                isAdd = true;
            }
        }
        if (isAdd) {
            return;
        }
        Optional<Dimension> dimensionOptional = getDimensionByName(dimension, datasource);
        if (dimensionOptional.isPresent()) {
            dataSet.getMeasure().add(DimensionNode.buildArray(dimensionOptional.get(), scope, engineType));
            addExtendFields(dimensionOptional.get(), extendFields);
            if (nonAgg) {
                output.getMeasure().add(DimensionNode.buildName(dimensionOptional.get(), scope, engineType));
                return;
            }
            output.getDimension().add(DimensionNode.buildName(dimensionOptional.get(), scope, engineType));
        }
    }

    private static void addExtendFields(Dimension dimension, Map<String, String> extendFields) {
        if (dimension.getDataType().isArray()) {
            if (Objects.nonNull(dimension.getExt()) && dimension.getExt()
                    .containsKey(DIMENSION_DELIMITER)) {
                extendFields.put(dimension.getExpr(),
                        (String) dimension.getExt().get(DIMENSION_DELIMITER));
            } else {
                extendFields.put(dimension.getExpr(), "");
            }
        }
    }

    private static List<SqlNode> getWhereMeasure(List<String> fields, List<String> queryMetrics,
            List<String> queryDimensions, Map<String, String> extendFields, DataSource datasource,
            SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        Iterator<String> iterator = fields.iterator();
        List<SqlNode> whereNode = new ArrayList<>();
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        while (iterator.hasNext()) {
            String cur = iterator.next();
            if (queryDimensions.contains(cur) || queryMetrics.contains(cur)) {
                iterator.remove();
            }
        }
        for (String where : fields) {
            List<Dimension> dimensionList = schema.getDimension().get(datasource.getName());
            boolean isAdd = false;
            if (!CollectionUtils.isEmpty(dimensionList)) {
                for (Dimension dim : dimensionList) {
                    if (!dim.getName().equalsIgnoreCase(where)) {
                        continue;
                    }
                    whereNode.addAll(DimensionNode.expand(dim, scope, engineType));
                    isAdd = true;
                }
            }
            Optional<Identify> identify = getIdentifyByName(where, datasource);
            if (identify.isPresent()) {
                whereNode.add(IdentifyNode.build(identify.get(), scope, engineType));
                isAdd = true;
            }
            if (isAdd) {
                continue;
            }
            Optional<Dimension> dimensionOptional = getDimensionByName(where, datasource);
            if (dimensionOptional.isPresent()) {
                whereNode.add(DimensionNode.buildArray(dimensionOptional.get(), scope, engineType));
                addExtendFields(dimensionOptional.get(), extendFields);
            }
        }
        return whereNode;
    }

    private static void mergeWhere(List<String> fields, TableView dataSet, TableView outputSet,
            List<String> queryMetrics,
            List<String> queryDimensions, Map<String, String> extendFields, DataSource datasource,
            SqlValidatorScope scope,
            SemanticSchema schema,
            boolean nonAgg) throws Exception {
        List<SqlNode> whereNode = getWhereMeasure(fields, queryMetrics, queryDimensions, extendFields, datasource,
                scope, schema,
                nonAgg);
        dataSet.getMeasure().addAll(whereNode);
        //getWhere(outputSet,fields,queryMetrics,queryDimensions,datasource,scope,schema);
    }

    public static void whereDimMetric(List<String> fields, List<String> queryMetrics,
            List<String> queryDimensions, DataSource datasource, SemanticSchema schema, Set<String> dimensions,
            Set<String> metrics) {
        for (String field : fields) {
            if (queryDimensions.contains(field) || queryMetrics.contains(field)) {
                continue;
            }
            String filterField = field;
            if (field.contains(Constants.DIMENSION_IDENTIFY)) {
                filterField = field.split(Constants.DIMENSION_IDENTIFY)[1];
            }
            addField(filterField, field, datasource, schema, dimensions, metrics);
        }
    }

    private static void addField(String field, String oriField, DataSource datasource, SemanticSchema schema,
            Set<String> dimensions,
            Set<String> metrics) {
        Optional<Dimension> dimension = datasource.getDimensions().stream()
                .filter(d -> d.getName().equalsIgnoreCase(field)).findFirst();
        if (dimension.isPresent()) {
            dimensions.add(oriField);
            return;
        }
        Optional<Identify> identify = datasource.getIdentifiers().stream()
                .filter(i -> i.getName().equalsIgnoreCase(field)).findFirst();
        if (identify.isPresent()) {
            dimensions.add(oriField);
            return;
        }
        if (schema.getDimension().containsKey(datasource.getName())) {
            Optional<Dimension> dataSourceDim = schema.getDimension().get(datasource.getName()).stream()
                    .filter(d -> d.getName().equalsIgnoreCase(field)).findFirst();
            if (dataSourceDim.isPresent()) {
                dimensions.add(oriField);
                return;
            }
        }
        Optional<Measure> metric = datasource.getMeasures()
                .stream().filter(m -> m.getName().equalsIgnoreCase(field)).findFirst();
        if (metric.isPresent()) {
            metrics.add(oriField);
            return;
        }
        Optional<Metric> datasourceMetric = schema.getMetrics()
                .stream().filter(m -> m.getName().equalsIgnoreCase(field)).findFirst();
        if (datasourceMetric.isPresent()) {
            Set<String> measures = datasourceMetric.get().getMetricTypeParams().getMeasures().stream()
                    .map(m -> m.getName()).collect(
                            Collectors.toSet());
            if (datasource.getMeasures().stream().map(m -> m.getName()).collect(Collectors.toSet())
                    .containsAll(measures)) {
                metrics.add(oriField);
                return;
            }
        }
    }

    public static boolean isDimension(String name, DataSource datasource, SemanticSchema schema) {
        Optional<Dimension> dimension = datasource.getDimensions().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
        if (dimension.isPresent()) {
            return true;
        }
        Optional<Identify> identify = datasource.getIdentifiers().stream()
                .filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return true;
        }
        if (schema.getDimension().containsKey(datasource.getName())) {
            Optional<Dimension> dataSourceDim = schema.getDimension().get(datasource.getName()).stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
            if (dataSourceDim.isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static void addTimeDimension(DataSource dataSource, List<String> queryDimension) {
        if (Materialization.TimePartType.ZIPPER.equals(dataSource.getTimePartType())) {
            Optional<Dimension> startTimeOp = dataSource.getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                    .filter(d -> d.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_START)).findFirst();
            Optional<Dimension> endTimeOp = dataSource.getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                    .filter(d -> d.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_END)).findFirst();
            if (startTimeOp.isPresent() && !queryDimension.contains(startTimeOp.get().getName())) {
                queryDimension.add(startTimeOp.get().getName());
            }
            if (endTimeOp.isPresent() && !queryDimension.contains(endTimeOp.get().getName())) {
                queryDimension.add(endTimeOp.get().getName());
            }
        } else {
            Optional<Dimension> timeOp = dataSource.getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType())).findFirst();
            if (timeOp.isPresent() && !queryDimension.contains(timeOp.get().getName())) {
                queryDimension.add(timeOp.get().getName());
            }
        }
    }

    public void render(MetricQueryParam metricQueryParam, List<DataSource> dataSources, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        String queryWhere = metricQueryParam.getWhere();
        Set<String> whereFields = new HashSet<>();
        List<String> fieldWhere = new ArrayList<>();
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        if (queryWhere != null && !queryWhere.isEmpty()) {
            SqlNode sqlNode = SemanticNode.parse(queryWhere, scope, engineType);
            FilterNode.getFilterField(sqlNode, whereFields);
            fieldWhere = whereFields.stream().collect(Collectors.toList());
        }
        if (dataSources.size() == 1) {
            DataSource dataSource = dataSources.get(0);
            super.tableView = renderOne("", fieldWhere, metricQueryParam.getMetrics(),
                    metricQueryParam.getDimensions(),
                    metricQueryParam.getWhere(), dataSource, scope, schema, nonAgg);
            return;
        }
        JoinRender joinRender = new JoinRender();
        joinRender.render(metricQueryParam, dataSources, scope, schema, nonAgg);
        super.tableView = joinRender.getTableView();
    }

}
