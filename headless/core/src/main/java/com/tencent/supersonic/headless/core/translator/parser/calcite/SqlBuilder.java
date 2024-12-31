package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.tencent.supersonic.common.calcite.Configuration;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.DataModelNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.render.FilterRender;
import com.tencent.supersonic.headless.core.translator.parser.calcite.render.OutputRender;
import com.tencent.supersonic.headless.core.translator.parser.calcite.render.Renderer;
import com.tencent.supersonic.headless.core.translator.parser.calcite.render.SourceRender;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.DataModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

@Slf4j
public class SqlBuilder {

    private final S2CalciteSchema schema;
    private OntologyQuery ontologyQuery;
    private SqlValidatorScope scope;
    private SqlNode parserNode;
    private boolean isAgg = false;
    private AggOption aggOption = AggOption.DEFAULT;

    public SqlBuilder(S2CalciteSchema schema) {
        this.schema = schema;
    }

    public String buildOntologySql(QueryStatement queryStatement) throws Exception {
        this.ontologyQuery = queryStatement.getOntologyQuery();
        if (ontologyQuery.getLimit() == null) {
            ontologyQuery.setLimit(0L);
        }
        this.aggOption = ontologyQuery.getAggOption();

        buildParseNode();
        optimizeParseNode(queryStatement.getOntology().getDatabaseType());
        return getSql(queryStatement.getOntology().getDatabaseType());
    }

    private void buildParseNode() throws Exception {
        // find relevant data models
        scope = SchemaBuilder.getScope(schema);
        List<DataModel> dataModels = DataModelNode.getQueryDataModels(scope, schema, ontologyQuery);
        if (dataModels == null || dataModels.isEmpty()) {
            throw new Exception("data model not found");
        }
        isAgg = getAgg(dataModels.get(0));

        // build level by level
        LinkedList<Renderer> builders = new LinkedList<>();
        builders.add(new SourceRender());
        builders.add(new FilterRender());
        builders.add(new OutputRender());
        ListIterator<Renderer> it = builders.listIterator();
        int i = 0;
        Renderer previous = null;
        while (it.hasNext()) {
            Renderer renderer = it.next();
            if (previous != null) {
                previous.render(ontologyQuery, dataModels, scope, schema, !isAgg);
                renderer.setTable(previous
                        .builderAs(DataModelNode.getNames(dataModels) + "_" + String.valueOf(i)));
                i++;
            }
            previous = renderer;
        }
        builders.getLast().render(ontologyQuery, dataModels, scope, schema, !isAgg);
        parserNode = builders.getLast().builder();
    }

    private boolean getAgg(DataModel dataModel) {
        if (!AggOption.DEFAULT.equals(aggOption)) {
            return AggOption.isAgg(aggOption);
        }
        // default by dataModel time aggregation
        if (Objects.nonNull(dataModel.getAggTime()) && !dataModel.getAggTime()
                .equalsIgnoreCase(Constants.DIMENSION_TYPE_TIME_GRANULARITY_NONE)) {
            if (!ontologyQuery.isNativeQuery()) {
                return true;
            }
        }
        return isAgg;
    }

    public String getSql(EngineType engineType) {
        return SemanticNode.getSql(parserNode, engineType);
    }

    private void optimizeParseNode(EngineType engineType) {
        if (Objects.isNull(schema.getRuntimeOptions())
                || Objects.isNull(schema.getRuntimeOptions().getEnableOptimize())
                || !schema.getRuntimeOptions().getEnableOptimize()) {
            return;
        }

        SqlNode optimizeNode = null;
        try {
            SqlNode sqlNode = SqlParser.create(SemanticNode.getSql(parserNode, engineType),
                    Configuration.getParserConfig(engineType)).parseStmt();
            if (Objects.nonNull(sqlNode)) {
                optimizeNode = SemanticNode.optimize(scope, schema, sqlNode, engineType);
            }
        } catch (Exception e) {
            log.error("optimize error {}", e);
        }

        if (Objects.nonNull(optimizeNode)) {
            parserNode = optimizeNode;
        }
    }

}
