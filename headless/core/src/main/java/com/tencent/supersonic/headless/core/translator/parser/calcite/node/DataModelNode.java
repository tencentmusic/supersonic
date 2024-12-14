package com.tencent.supersonic.headless.core.translator.parser.calcite.node;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.calcite.Configuration;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.core.pojo.DataModel;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.SchemaBuilder;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DataModelNode extends SemanticNode {

    public static SqlNode build(DataModel dataModel, SqlValidatorScope scope) throws Exception {
        String sqlTable = "";
        if (dataModel.getSqlQuery() != null && !dataModel.getSqlQuery().isEmpty()) {
            sqlTable = dataModel.getSqlQuery();
        } else if (dataModel.getTableQuery() != null && !dataModel.getTableQuery().isEmpty()) {
            if (dataModel.getType().equalsIgnoreCase(EngineType.POSTGRESQL.getName())) {
                String fullTableName =
                        String.join(".public.", dataModel.getTableQuery().split("\\."));
                sqlTable = "select * from " + fullTableName;
            } else {
                sqlTable = "select * from " + dataModel.getTableQuery();
            }
        }
        if (sqlTable.isEmpty()) {
            throw new Exception("DataModelNode build error [tableSqlNode not found]");
        }
        SqlNode source = getTable(sqlTable, scope, EngineType.fromString(dataModel.getType()));
        addSchema(scope, dataModel, sqlTable);
        return buildAs(dataModel.getName(), source);
    }

    private static void addSchema(SqlValidatorScope scope, DataModel datasource, String table)
            throws Exception {
        Map<String, Set<String>> sqlTable = SqlSelectHelper.getFieldsWithSubQuery(table);
        for (Map.Entry<String, Set<String>> entry : sqlTable.entrySet()) {
            String tb = entry.getKey();
            String db = "";
            if (entry.getKey().indexOf(".") > 0) {
                db = entry.getKey().substring(0, entry.getKey().lastIndexOf("."));
                tb = entry.getKey().substring(entry.getKey().lastIndexOf(".") + 1);
            }
            addSchemaTable(scope, datasource, db, tb, entry.getValue());
        }
    }

    private static void addSchemaTable(SqlValidatorScope scope, DataModel datasource, String db,
            String tb, Set<String> fields) throws Exception {
        Set<String> dateInfo = new HashSet<>();
        Set<String> dimensions = new HashSet<>();
        Set<String> metrics = new HashSet<>();
        EngineType engineType = EngineType.fromString(datasource.getType());
        for (DimSchemaResp d : datasource.getDimensions()) {
            List<SqlNode> identifiers =
                    expand(SemanticNode.parse(d.getExpr(), scope, engineType), scope);
            identifiers.forEach(i -> dimensions.add(i.toString()));
            dimensions.add(d.getName());
        }
        for (Identify i : datasource.getIdentifiers()) {
            dimensions.add(i.getName());
        }
        for (Measure m : datasource.getMeasures()) {
            List<SqlNode> identifiers =
                    expand(SemanticNode.parse(m.getExpr(), scope, engineType), scope);
            identifiers.forEach(i -> {
                if (!dimensions.contains(i.toString())) {
                    metrics.add(i.toString());
                }
            });
            if (!dimensions.contains(m.getName())) {
                metrics.add(m.getName());
            }
        }
        for (String field : fields) {
            if (!metrics.contains(field) && !dimensions.contains(field)) {
                dimensions.add(field);
                log.info("add column {} {}", datasource.getName(), field);
            }
        }
        SchemaBuilder.addSourceView(scope.getValidator().getCatalogReader().getRootSchema(), db, tb,
                dateInfo, dimensions, metrics);
    }

    public static SqlNode buildExtend(DataModel datasource, Map<String, String> exprList,
            SqlValidatorScope scope) throws Exception {
        if (CollectionUtils.isEmpty(exprList)) {
            return build(datasource, scope);
        }
        EngineType engineType = EngineType.fromString(datasource.getType());
        SqlNode dataSet = new SqlBasicCall(new LateralViewExplodeNode(exprList),
                Arrays.asList(build(datasource, scope), new SqlNodeList(
                        getExtendField(exprList, scope, engineType), SqlParserPos.ZERO)),
                SqlParserPos.ZERO);
        return buildAs(datasource.getName() + Constants.DIMENSION_ARRAY_SINGLE_SUFFIX, dataSet);
    }

    public static List<SqlNode> getExtendField(Map<String, String> exprList,
            SqlValidatorScope scope, EngineType engineType) throws Exception {
        List<SqlNode> sqlNodeList = new ArrayList<>();
        for (String expr : exprList.keySet()) {
            sqlNodeList.add(parse(expr, scope, engineType));
            sqlNodeList.add(new SqlDataTypeSpec(
                    new SqlUserDefinedTypeNameSpec(expr + Constants.DIMENSION_ARRAY_SINGLE_SUFFIX,
                            SqlParserPos.ZERO),
                    SqlParserPos.ZERO));
        }
        return sqlNodeList;
    }

    private static SqlNode getTable(String sqlQuery, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        SqlParser sqlParser = SqlParser.create(sqlQuery, Configuration.getParserConfig(engineType));
        SqlNode sqlNode = sqlParser.parseQuery();
        scope.validateExpr(sqlNode);
        return sqlNode;
    }

    public static String getNames(List<DataModel> dataModelList) {
        return dataModelList.stream().map(DataModel::getName).collect(Collectors.joining("_"));
    }

    public static void getQueryDimensionMeasure(Ontology ontology, OntologyQuery ontologyQuery,
            Set<String> queryDimensions, Set<String> queryMeasures) {
        ontologyQuery.getMetrics().forEach(m -> {
            if (Objects.nonNull(m.getMetricDefineByMeasureParams())) {
                m.getMetricDefineByMeasureParams().getMeasures()
                        .forEach(mm -> queryMeasures.add(mm.getName()));
            }
            if (Objects.nonNull(m.getMetricDefineByFieldParams())) {
                m.getMetricDefineByFieldParams().getFields()
                        .forEach(mm -> queryMeasures.add(mm.getFieldName()));
            }
        });
    }

    public static void mergeQueryFilterDimensionMeasure(Ontology ontology,
            OntologyQuery ontologyQuery, Set<String> dimensions, Set<String> measures,
            SqlValidatorScope scope) throws Exception {
        EngineType engineType = ontology.getDatabase().getType();
        if (Objects.nonNull(ontologyQuery.getWhere()) && !ontologyQuery.getWhere().isEmpty()) {
            Set<String> filterConditions = new HashSet<>();
            FilterNode.getFilterField(parse(ontologyQuery.getWhere(), scope, engineType),
                    filterConditions);
            Set<String> queryMeasures = new HashSet<>(measures);
            Set<String> schemaMetricName = ontology.getMetrics().stream()
                    .map(MetricSchemaResp::getName).collect(Collectors.toSet());
            for (String filterCondition : filterConditions) {
                if (schemaMetricName.contains(filterCondition)) {
                    ontology.getMetrics().stream()
                            .filter(m -> m.getName().equalsIgnoreCase(filterCondition))
                            .forEach(m -> m.getMetricDefineByMeasureParams().getMeasures()
                                    .forEach(mm -> queryMeasures.add(mm.getName())));
                    continue;
                }
                dimensions.add(filterCondition);
            }
            measures.clear();
            measures.addAll(queryMeasures);
        }
    }

    public static List<DataModel> getQueryDataModelsV2(Ontology ontology, OntologyQuery query) {
        // first, sort models based on the number of query metrics
        Map<String, Integer> modelMetricCount = Maps.newHashMap();
        query.getMetrics().forEach(m -> {
            if (!modelMetricCount.containsKey(m.getModelBizName())) {
                modelMetricCount.put(m.getModelBizName(), 1);
            } else {
                int count = modelMetricCount.get(m.getModelBizName());
                modelMetricCount.put(m.getModelBizName(), count + 1);
            }
        });
        List<String> metricsDataModels = modelMetricCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(e -> e.getKey())
                .collect(Collectors.toList());

        // first, sort models based on the number of query dimensions
        Map<String, Integer> modelDimCount = Maps.newHashMap();
        query.getDimensions().forEach(m -> {
            if (!modelDimCount.containsKey(m.getModelBizName())) {
                modelDimCount.put(m.getModelBizName(), 1);
            } else {
                int count = modelDimCount.get(m.getModelBizName());
                modelDimCount.put(m.getModelBizName(), count + 1);
            }
        });
        List<String> dimDataModels = modelDimCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(e -> e.getKey())
                .collect(Collectors.toList());

        Set<String> dataModelNames = Sets.newLinkedHashSet();
        dataModelNames.addAll(metricsDataModels);
        dataModelNames.addAll(dimDataModels);
        return dataModelNames.stream().map(bizName -> ontology.getDataModelMap().get(bizName))
                .collect(Collectors.toList());
    }

    public static List<DataModel> getQueryDataModels(Ontology ontology,
            OntologyQuery ontologyQuery) {
        // get query measures and dimensions
        Set<String> queryMeasures = new HashSet<>();
        Set<String> queryDimensions = new HashSet<>();
        getQueryDimensionMeasure(ontology, ontologyQuery, queryDimensions, queryMeasures);

        // first, find the base model
        DataModel baseDataModel = findBaseModel(ontology, ontologyQuery);
        if (Objects.isNull(baseDataModel)) {
            throw new RuntimeException(
                    String.format("could not find matching dataModel, dimensions:%s, measures:%s",
                            queryDimensions, queryMeasures));
        }
        // if the base model matches all queried measures and dimensions, just return
        if (checkMatch(baseDataModel, queryMeasures, queryDimensions)) {
            log.debug("baseDataModel match all measures and dimensions");
            return Collections.singletonList(baseDataModel);
        }

        // second, traverse the ontology to find other related dataModels
        List<DataModel> relatedDataModels = findRelatedModelsByRelation(ontology, ontologyQuery,
                baseDataModel, queryDimensions, queryMeasures);
        if (CollectionUtils.isEmpty(relatedDataModels)) {
            relatedDataModels = findRelatedModelsByIdentifier(ontology, baseDataModel,
                    queryDimensions, queryMeasures);
        }
        if (CollectionUtils.isEmpty(relatedDataModels)) {
            relatedDataModels = Collections.singletonList(baseDataModel);
        }

        log.debug("relatedDataModels {}", relatedDataModels);
        return relatedDataModels;
    }

    private static DataModel findBaseModel(Ontology ontology, OntologyQuery query) {
        DataModel dataModel = null;
        // first, try to find the model with the most query metrics
        Map<String, Integer> modelMetricCount = Maps.newHashMap();
        query.getMetrics().forEach(m -> {
            if (!modelMetricCount.containsKey(m.getModelBizName())) {
                modelMetricCount.put(m.getModelBizName(), 1);
            } else {
                int count = modelMetricCount.get(m.getModelBizName());
                modelMetricCount.put(m.getModelBizName(), count + 1);
            }
        });
        Optional<String> baseModelName = modelMetricCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(e -> e.getKey())
                .findFirst();
        if (baseModelName.isPresent()) {
            dataModel = ontology.getDataModelMap().get(baseModelName.get());
        } else {
            // second, try to find the model with the most query dimensions
            Map<String, Integer> modelDimCount = Maps.newHashMap();
            query.getDimensions().forEach(m -> {
                if (!modelDimCount.containsKey(m.getModelBizName())) {
                    modelDimCount.put(m.getModelBizName(), 1);
                } else {
                    int count = modelDimCount.get(m.getModelBizName());
                    modelDimCount.put(m.getModelBizName(), count + 1);
                }
            });
            baseModelName = modelMetricCount.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(e -> e.getKey()).findFirst();
            if (baseModelName.isPresent()) {
                dataModel = ontology.getDataModelMap().get(baseModelName.get());
            }
        }

        return dataModel;
    }

    private static DataModel findBaseModel(Ontology ontology, Set<String> queryMeasures,
            Set<String> queryDimensions) {
        DataModel dataModel = null;
        // first, try to find the model with the most matching measures
        Map<String, Integer> dataModelMeasuresCount = new HashMap<>();
        for (Map.Entry<String, DataModel> entry : ontology.getDataModelMap().entrySet()) {
            Set<String> sourceMeasure = entry.getValue().getMeasures().stream()
                    .map(Measure::getName).collect(Collectors.toSet());
            sourceMeasure.retainAll(queryMeasures);
            dataModelMeasuresCount.put(entry.getKey(), sourceMeasure.size());
        }
        log.info("dataModelMeasureCount: [{}]", dataModelMeasuresCount);
        Optional<Map.Entry<String, Integer>> base =
                dataModelMeasuresCount.entrySet().stream().filter(e -> e.getValue() > 0)
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).findFirst();

        if (base.isPresent()) {
            dataModel = ontology.getDataModelMap().get(base.get().getKey());
        } else {
            // second, try to find the model with the most matching dimensions
            Map<String, Integer> dataModelDimCount = new HashMap<>();
            for (Map.Entry<String, List<DimSchemaResp>> entry : ontology.getDimensionMap()
                    .entrySet()) {
                Set<String> modelDimensions = entry.getValue().stream().map(DimSchemaResp::getName)
                        .collect(Collectors.toSet());
                modelDimensions.retainAll(queryDimensions);
                dataModelDimCount.put(entry.getKey(), modelDimensions.size());
            }
            log.info("dataModelDimCount: [{}]", dataModelDimCount);
            base = dataModelDimCount.entrySet().stream().filter(e -> e.getValue() > 0)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).findFirst();
            if (base.isPresent()) {
                dataModel = ontology.getDataModelMap().get(base.get().getKey());
            }
        }

        return dataModel;
    }

    private static boolean checkMatch(DataModel baseDataModel, Set<String> queryMeasures,
            Set<String> queryDimension) {
        boolean isAllMatch = true;
        Set<String> baseMeasures = baseDataModel.getMeasures().stream().map(Measure::getName)
                .collect(Collectors.toSet());
        Set<String> baseDimensions = baseDataModel.getDimensions().stream()
                .map(DimSchemaResp::getName).collect(Collectors.toSet());
        baseDataModel.getIdentifiers().forEach(i -> baseDimensions.add(i.getName()));

        baseMeasures.retainAll(queryMeasures);
        if (baseMeasures.size() < queryMeasures.size()) {
            // check dimension again
            Set<String> dimensionMeasures = new HashSet<>();
            dimensionMeasures.addAll(baseDimensions);
            dimensionMeasures.retainAll(queryMeasures);
            if (baseMeasures.size() + dimensionMeasures.size() < queryMeasures.size()) {
                log.info("baseDataModel not match all measures");
                isAllMatch = false;
            }
            queryMeasures.removeAll(dimensionMeasures);
        }
        queryMeasures.removeAll(baseMeasures);

        baseDimensions.retainAll(queryDimension);
        if (baseDimensions.size() < queryDimension.size()) {
            log.debug("baseDataModel not match all dimensions");
            isAllMatch = false;
        }
        queryDimension.removeAll(baseDimensions);

        return isAllMatch;
    }

    private static List<DataModel> findRelatedModelsByRelation(Ontology ontology,
            OntologyQuery ontologyQuery, DataModel baseDataModel, Set<String> queryDimensions,
            Set<String> queryMeasures) {
        Set<String> joinDataModelNames = new HashSet<>();
        List<DataModel> joinDataModels = new ArrayList<>();
        Set<String> before = new HashSet<>();
        before.add(baseDataModel.getName());

        if (!CollectionUtils.isEmpty(ontology.getJoinRelations())) {
            Set<Long> visitJoinRelations = new HashSet<>();
            List<JoinRelation> sortedJoinRelation = new ArrayList<>();
            sortJoinRelation(ontology.getJoinRelations(), baseDataModel.getName(),
                    visitJoinRelations, sortedJoinRelation);
            ontology.getJoinRelations().stream()
                    .filter(j -> !visitJoinRelations.contains(j.getId()))
                    .forEach(sortedJoinRelation::add);
            for (JoinRelation joinRelation : sortedJoinRelation) {
                if (!before.contains(joinRelation.getLeft())
                        && !before.contains(joinRelation.getRight())) {
                    continue;
                }
                boolean isMatch = false;
                boolean isRight = before.contains(joinRelation.getLeft());
                DataModel other = isRight ? ontology.getDataModelMap().get(joinRelation.getRight())
                        : ontology.getDataModelMap().get(joinRelation.getLeft());
                String joinDimName = isRight ? joinRelation.getJoinCondition().get(0).getRight()
                        : joinRelation.getJoinCondition().get(0).getLeft();
                if (!queryDimensions.isEmpty()) {
                    Set<String> linkDimension = other.getDimensions().stream()
                            .map(DimSchemaResp::getName).collect(Collectors.toSet());
                    other.getIdentifiers().forEach(i -> linkDimension.add(i.getName()));
                    linkDimension.retainAll(queryDimensions);
                    if (!linkDimension.isEmpty()) {
                        isMatch = true;
                        // joinDim should be added to the query dimension
                        // ontologyQuery.getDimensions().add(joinDimName);
                    }
                }
                Set<String> linkMeasure = other.getMeasures().stream().map(Measure::getName)
                        .collect(Collectors.toSet());
                linkMeasure.retainAll(queryMeasures);
                if (!linkMeasure.isEmpty()) {
                    isMatch = true;
                }
                if (!isMatch && ontology.getDimensionMap().containsKey(other.getName())) {
                    Set<String> linkDimension = ontology.getDimensionMap().get(other.getName())
                            .stream().map(DimSchemaResp::getName).collect(Collectors.toSet());
                    linkDimension.retainAll(queryDimensions);
                    if (!linkDimension.isEmpty()) {
                        isMatch = true;
                    }
                }
                if (isMatch) {
                    joinDataModelNames.add(other.getName());
                    before.add(other.getName());
                }
            }
        }
        if (!CollectionUtils.isEmpty(joinDataModelNames)) {
            Map<String, Long> orders = new HashMap<>();
            joinDataModelNames.add(baseDataModel.getName());
            orders.put(baseDataModel.getName(), 0L);

            // Adjust the order of tables in the data source to facilitate subsequent joins
            ArrayList<String> joinTables = new ArrayList<>();
            for (JoinRelation joinRelation : ontology.getJoinRelations()) {
                if (joinDataModelNames.contains(joinRelation.getLeft())
                        && joinDataModelNames.contains(joinRelation.getRight())) {
                    joinTables.add(joinRelation.getLeft());
                    joinTables.add(joinRelation.getRight());
                }
            }
            for (String joinTable : joinTables) {
                orders.put(joinTable, orders.getOrDefault(joinTable, 0L) + 1L);
            }
            orders.entrySet().stream()
                    .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // 倒序排序
                    .forEach(d -> {
                        joinDataModels.add(ontology.getDataModelMap().get(d.getKey()));
                    });
        }
        return joinDataModels;
    }

    private static void sortJoinRelation(List<JoinRelation> joinRelations, String next,
            Set<Long> visited, List<JoinRelation> sortedJoins) {
        for (JoinRelation link : joinRelations) {
            if (!visited.contains(link.getId())) {
                if (link.getLeft().equals(next) || link.getRight().equals(next)) {
                    visited.add(link.getId());
                    sortedJoins.add(link);
                    sortJoinRelation(joinRelations,
                            link.getLeft().equals(next) ? link.getRight() : link.getLeft(), visited,
                            sortedJoins);
                }
            }
        }
    }

    private static List<DataModel> findRelatedModelsByIdentifier(Ontology ontology,
            DataModel baseDataModel, Set<String> queryDimension, Set<String> measures) {
        Set<String> baseIdentifiers = baseDataModel.getIdentifiers().stream().map(Identify::getName)
                .collect(Collectors.toSet());
        if (baseIdentifiers.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Set<String> linkDataSourceName = new HashSet<>();
        List<DataModel> linkDataModels = new ArrayList<>();
        for (Map.Entry<String, DataModel> entry : ontology.getDataModelMap().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(baseDataModel.getName())) {
                continue;
            }
            long identifierNum = entry.getValue().getIdentifiers().stream().map(Identify::getName)
                    .filter(baseIdentifiers::contains).count();
            if (identifierNum > 0) {
                boolean isMatch = false;
                if (!queryDimension.isEmpty()) {
                    Set<String> linkDimension = entry.getValue().getDimensions().stream()
                            .map(DimSchemaResp::getName).collect(Collectors.toSet());
                    entry.getValue().getIdentifiers().forEach(i -> linkDimension.add(i.getName()));
                    linkDimension.retainAll(queryDimension);
                    if (!linkDimension.isEmpty()) {
                        isMatch = true;
                    }
                }
                if (!measures.isEmpty()) {
                    Set<String> linkMeasure = entry.getValue().getMeasures().stream()
                            .map(Measure::getName).collect(Collectors.toSet());
                    linkMeasure.retainAll(measures);
                    if (!linkMeasure.isEmpty()) {
                        isMatch = true;
                    }
                }
                if (isMatch) {
                    linkDataSourceName.add(entry.getKey());
                }
            }
        }
        for (Map.Entry<String, List<DimSchemaResp>> entry : ontology.getDimensionMap().entrySet()) {
            if (!queryDimension.isEmpty()) {
                Set<String> linkDimension = entry.getValue().stream().map(DimSchemaResp::getName)
                        .collect(Collectors.toSet());
                linkDimension.retainAll(queryDimension);
                if (!linkDimension.isEmpty()) {
                    linkDataSourceName.add(entry.getKey());
                }
            }
        }
        for (String linkName : linkDataSourceName) {
            linkDataModels.add(ontology.getDataModelMap().get(linkName));
        }
        if (!CollectionUtils.isEmpty(linkDataModels)) {
            List<DataModel> all = new ArrayList<>();
            all.add(baseDataModel);
            all.addAll(linkDataModels);
            return all;
        }
        return Lists.newArrayList();
    }

}
