package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.google.common.collect.Sets;
import com.tencent.supersonic.common.calcite.Configuration;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.parser.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SqlBuilder {

    private final S2CalciteSchema schema;
    private final SqlValidatorScope scope;

    public SqlBuilder(S2CalciteSchema schema) {
        this.schema = schema;
        this.scope = SchemaBuilder.getScope(schema);
    }

    public String buildOntologySql(QueryStatement queryStatement) throws Exception {
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();
        Ontology ontology = queryStatement.getOntology();

        if (ontologyQuery.getLimit() == null) {
            ontologyQuery.setLimit(0L);
        }

        Set<ModelResp> dataModels = ontologyQuery.getModels();
        if (dataModels == null || dataModels.isEmpty()) {
            throw new Exception("data model not found");
        }

        TableView tableView;
        if (!CollectionUtils.isEmpty(ontology.getJoinRelations()) && dataModels.size() > 1) {
            Set<ModelResp> models = probeRelatedModels(dataModels, queryStatement.getOntology());
            tableView = render(ontologyQuery, models, scope, schema);
        } else {
            tableView = render(ontologyQuery, dataModels, scope, schema);
        }

        SqlNode parserNode = tableView.build();
        DatabaseResp database = queryStatement.getOntology().getDatabase();
        EngineType engineType = EngineType.fromString(database.getType());
        try {
            parserNode = optimizeParseNode(parserNode, engineType);
        } catch (Exception e) {
            // failure in optimization phase doesn't affect the query result,
            // just ignore it
            log.error("optimizeParseNode error", e);
        }
        return SemanticNode.getSql(parserNode, engineType);
    }

    private Set<ModelResp> probeRelatedModels(Set<ModelResp> dataModels, Ontology ontology) {
        List<JoinRelation> joinRelations = ontology.getJoinRelations();
        Graph<String, DefaultEdge> graph = buildGraph(joinRelations);
        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        Set<String> queryModels =
                dataModels.stream().map(ModelResp::getName).collect(Collectors.toSet());
        GraphPath<String, DefaultEdge> selectedGraphPath = null;
        for (String fromModel : queryModels) {
            for (String toModel : queryModels) {
                if (fromModel != toModel) {
                    GraphPath<String, DefaultEdge> path = dijkstraAlg.getPath(fromModel, toModel);
                    if (isGraphPathContainsAll(path, queryModels)) {
                        selectedGraphPath = path;
                        break;
                    }
                }
            }
        }
        if (selectedGraphPath == null) {
            return dataModels;
        }
        Set<String> modelNames = Sets.newHashSet();
        for (DefaultEdge edge : selectedGraphPath.getEdgeList()) {
            modelNames.add(selectedGraphPath.getGraph().getEdgeSource(edge));
            modelNames.add(selectedGraphPath.getGraph().getEdgeTarget(edge));
        }
        return modelNames.stream().map(m -> ontology.getModelMap().get(m))
                .collect(Collectors.toSet());
    }

    private boolean isGraphPathContainsAll(GraphPath<String, DefaultEdge> graphPath,
            Set<String> vertex) {
        Set<String> allVertex = Sets.newHashSet();
        for (DefaultEdge edge : graphPath.getEdgeList()) {
            allVertex.add(graphPath.getGraph().getEdgeSource(edge));
            allVertex.add(graphPath.getGraph().getEdgeTarget(edge));
        }
        Collection<String> intersect =
                org.apache.commons.collections.CollectionUtils.intersection(vertex, allVertex);

        return intersect.size() == vertex.size() ? true : false;
    }

    private Graph<String, DefaultEdge> buildGraph(List<JoinRelation> joinRelations) {
        Graph<String, DefaultEdge> directedGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (JoinRelation joinRelation : joinRelations) {
            directedGraph.addVertex(joinRelation.getLeft());
            directedGraph.addVertex(joinRelation.getRight());
            directedGraph.addEdge(joinRelation.getLeft(), joinRelation.getRight());
        }
        return directedGraph;
    }

    private SqlNode optimizeParseNode(SqlNode parserNode, EngineType engineType)
            throws SqlParseException {
        if (Objects.isNull(schema.getRuntimeOptions())
                || Objects.isNull(schema.getRuntimeOptions().getEnableOptimize())
                || !schema.getRuntimeOptions().getEnableOptimize()) {
            return parserNode;
        }

        SqlNode optimizeNode = null;
        SqlNode sqlNode = SqlParser.create(SemanticNode.getSql(parserNode, engineType),
                Configuration.getParserConfig(engineType)).parseStmt();
        if (Objects.nonNull(sqlNode)) {
            optimizeNode = SemanticNode.optimize(scope, schema, sqlNode, engineType);
        }

        if (Objects.nonNull(optimizeNode)) {
            return optimizeNode;
        }

        return parserNode;
    }

    private TableView render(OntologyQuery ontologyQuery, Set<ModelResp> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema) throws Exception {
        SqlNode left = null;
        TableView leftTable = null;
        TableView outerTable = new TableView();
        Map<String, SqlNode> outerSelect = new HashMap<>();
        Map<String, String> beforeModels = new HashMap<>();
        EngineType engineType = EngineType.fromString(schema.getOntology().getDatabase().getType());

        for (ModelResp dataModel : dataModels) {
            final Set<DimSchemaResp> queryDimensions =
                    ontologyQuery.getDimensionsByModel(dataModel.getName());
            final Set<MetricSchemaResp> queryMetrics =
                    ontologyQuery.getMetricsByModel(dataModel.getName());

            List<String> primary = new ArrayList<>();
            for (Identify identify : dataModel.getIdentifiers()) {
                primary.add(identify.getName());
            }

            TableView tableView =
                    renderOne(queryMetrics, queryDimensions, dataModel, scope, schema);
            log.info("tableView {}", StringUtils.normalizeSpace(tableView.getTable().toString()));
            String alias = Constants.JOIN_TABLE_PREFIX + dataModel.getName();
            tableView.setAlias(alias);
            tableView.setPrimary(primary);
            tableView.setDataModel(dataModel);
            for (String field : tableView.getFields()) {
                outerSelect.put(field, SemanticNode.parse(alias + "." + field, scope, engineType));
            }
            if (left == null) {
                left = SemanticNode.buildAs(tableView.getAlias(), getTable(tableView));
            } else {
                left = buildJoin(left, leftTable, tableView, beforeModels, dataModel, schema,
                        scope);
            }
            leftTable = tableView;
            beforeModels.put(dataModel.getName(), leftTable.getAlias());
        }

        for (Map.Entry<String, SqlNode> entry : outerSelect.entrySet()) {
            outerTable.getSelect().add(entry.getValue());
        }
        outerTable.setTable(left);

        return outerTable;
    }

    private SqlNode getTable(TableView tableView) {
        return SemanticNode.getTable(tableView.getTable());
    }

    private SqlNode buildJoin(SqlNode leftNode, TableView leftTable, TableView rightTable,
            Map<String, String> before, ModelResp dataModel, S2CalciteSchema schema,
            SqlValidatorScope scope) throws Exception {
        EngineType engineType = EngineType.fromString(schema.getOntology().getDatabase().getType());
        SqlNode condition =
                getCondition(leftTable, rightTable, dataModel, schema, scope, engineType);
        SqlLiteral sqlLiteral = SemanticNode.getJoinSqlLiteral("");
        JoinRelation matchJoinRelation = getMatchJoinRelation(before, rightTable, schema);
        SqlNode joinRelationCondition;
        if (!org.apache.commons.collections.CollectionUtils
                .isEmpty(matchJoinRelation.getJoinCondition())) {
            sqlLiteral = SemanticNode.getJoinSqlLiteral(matchJoinRelation.getJoinType());
            joinRelationCondition = getCondition(matchJoinRelation, scope, engineType);
            condition = joinRelationCondition;
        }

        return new SqlJoin(SqlParserPos.ZERO, leftNode,
                SqlLiteral.createBoolean(false, SqlParserPos.ZERO), sqlLiteral,
                SemanticNode.buildAs(rightTable.getAlias(), getTable(rightTable)),
                SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO), condition);
    }

    private JoinRelation getMatchJoinRelation(Map<String, String> before, TableView tableView,
            S2CalciteSchema schema) {
        JoinRelation matchJoinRelation = JoinRelation.builder().build();
        if (!CollectionUtils.isEmpty(schema.getJoinRelations())) {
            for (JoinRelation joinRelation : schema.getJoinRelations()) {
                if (joinRelation.getRight().equalsIgnoreCase(tableView.getDataModel().getName())
                        && before.containsKey(joinRelation.getLeft())) {
                    matchJoinRelation.setJoinCondition(joinRelation.getJoinCondition().stream()
                            .map(r -> Triple.of(
                                    before.get(joinRelation.getLeft()) + "." + r.getLeft(),
                                    r.getMiddle(), tableView.getAlias() + "." + r.getRight()))
                            .collect(Collectors.toList()));
                    matchJoinRelation.setJoinType(joinRelation.getJoinType());
                    // Added join condition judgment to solve the problem of join condition order
                } else if (joinRelation.getLeft()
                        .equalsIgnoreCase(tableView.getDataModel().getName())
                        && before.containsKey(joinRelation.getRight())) {
                    List<Triple<String, String, String>> candidateJoinCon = joinRelation
                            .getJoinCondition().stream()
                            .map(r -> Triple.of(
                                    before.get(joinRelation.getRight()) + "." + r.getRight(),
                                    r.getMiddle(), tableView.getAlias() + "." + r.getLeft()))
                            .collect(Collectors.toList());
                    // added by jerryjzhang on 20250214
                    // use the one with the most conditions to join left and right tables
                    if (matchJoinRelation.getJoinCondition() == null || candidateJoinCon
                            .size() > matchJoinRelation.getJoinCondition().size()) {
                        matchJoinRelation.setJoinCondition(candidateJoinCon);
                        matchJoinRelation.setJoinType(joinRelation.getJoinType());
                    }
                }
            }
        }
        return matchJoinRelation;
    }

    private SqlNode getCondition(JoinRelation joinRelation, SqlValidatorScope scope,
            EngineType engineType) throws Exception {
        SqlNode condition = null;
        for (Triple<String, String, String> con : joinRelation.getJoinCondition()) {
            List<SqlNode> ons = new ArrayList<>();
            ons.add(SemanticNode.parse(con.getLeft(), scope, engineType));
            ons.add(SemanticNode.parse(con.getRight(), scope, engineType));
            if (Objects.isNull(condition)) {
                condition = new SqlBasicCall(SemanticNode.getBinaryOperator(con.getMiddle()), ons,
                        SqlParserPos.ZERO, null);
                continue;
            }
            SqlNode addCondition = new SqlBasicCall(SemanticNode.getBinaryOperator(con.getMiddle()),
                    ons, SqlParserPos.ZERO, null);
            condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                    new ArrayList<>(Arrays.asList(condition, addCondition)), SqlParserPos.ZERO,
                    null);
        }
        return condition;
    }

    private SqlNode getCondition(TableView left, TableView right, ModelResp dataModel,
            S2CalciteSchema schema, SqlValidatorScope scope, EngineType engineType)
            throws Exception {

        Set<String> selectLeft = SemanticNode.getSelect(left.getTable());
        Set<String> selectRight = SemanticNode.getSelect(right.getTable());
        selectLeft.retainAll(selectRight);
        SqlNode condition = null;
        for (String on : selectLeft) {
            if (!isDimension(on, dataModel, schema)) {
                continue;
            }
            if (isForeign(on, left.getDataModel().getIdentifiers())) {
                if (!isPrimary(on, right.getDataModel().getIdentifiers())) {
                    continue;
                }
            }
            if (isForeign(on, right.getDataModel().getIdentifiers())) {
                if (!isPrimary(on, left.getDataModel().getIdentifiers())) {
                    continue;
                }
            }
            List<SqlNode> ons = new ArrayList<>();
            ons.add(SemanticNode.parse(left.getAlias() + "." + on, scope, engineType));
            ons.add(SemanticNode.parse(right.getAlias() + "." + on, scope, engineType));
            if (condition == null) {
                condition =
                        new SqlBasicCall(SqlStdOperatorTable.EQUALS, ons, SqlParserPos.ZERO, null);
                continue;
            }
            SqlNode addCondition =
                    new SqlBasicCall(SqlStdOperatorTable.EQUALS, ons, SqlParserPos.ZERO, null);
            condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                    new ArrayList<>(Arrays.asList(condition, addCondition)), SqlParserPos.ZERO,
                    null);
        }
        return condition;
    }

    public static TableView renderOne(Set<MetricSchemaResp> queryMetrics,
            Set<DimSchemaResp> queryDimensions, ModelResp dataModel, SqlValidatorScope scope,
            S2CalciteSchema schema) {
        TableView tableView = new TableView();
        EngineType engineType = EngineType.fromString(schema.getOntology().getDatabase().getType());
        Set<String> queryFields = tableView.getFields();
        if (Objects.nonNull(queryMetrics)) {
            queryMetrics.stream().forEach(m -> queryFields.addAll(m.getFields()));
        }
        if (Objects.nonNull(queryDimensions)) {
            queryDimensions.stream().forEach(d -> queryFields.addAll(d.getFields()));
        }

        try {
            for (String field : queryFields) {
                tableView.getSelect().add(SemanticNode.parse(field, scope, engineType));
            }
            tableView.setTable(DataModelNode.build(dataModel, scope));
        } catch (Exception e) {
            log.error("Failed to create sqlNode for data model {}", dataModel);
        }

        return tableView;
    }

    private static boolean isDimension(String name, ModelResp dataModel, S2CalciteSchema schema) {
        Optional<Dimension> dimension = dataModel.getModelDetail().getDimensions().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
        if (dimension.isPresent()) {
            return true;
        }
        Optional<Identify> identify = dataModel.getIdentifiers().stream()
                .filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return true;
        }
        if (schema.getDimensions().containsKey(dataModel.getName())) {
            Optional<DimSchemaResp> dataSourceDim = schema.getDimensions().get(dataModel.getName())
                    .stream().filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
            if (dataSourceDim.isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isForeign(String name, List<Identify> identifies) {
        Optional<Identify> identify =
                identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return IdentifyType.foreign.equals(identify.get().getType());
        }
        return false;
    }

    private static boolean isPrimary(String name, List<Identify> identifies) {
        Optional<Identify> identify =
                identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return IdentifyType.primary.equals(identify.get().getType());
        }
        return false;
    }

}
