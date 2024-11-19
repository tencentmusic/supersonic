package com.tencent.supersonic.headless.core.translator.calcite.sql;

import com.tencent.supersonic.common.calcite.Configuration;
import com.tencent.supersonic.common.calcite.SqlMergeWithUtils;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataModel;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.DataModelNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.render.FilterRender;
import com.tencent.supersonic.headless.core.translator.calcite.sql.render.OutputRender;
import com.tencent.supersonic.headless.core.translator.calcite.sql.render.Renderer;
import com.tencent.supersonic.headless.core.translator.calcite.sql.render.SourceRender;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/** parsing from query dimensions and metrics */
@Slf4j
public class SqlBuilder {

    private MetricQueryParam metricQueryParam;
    private final S2CalciteSchema schema;
    private SqlValidatorScope scope;
    private SqlNode parserNode;
    private boolean isAgg = false;
    private AggOption aggOption = AggOption.DEFAULT;

    public SqlBuilder(S2CalciteSchema schema) {
        this.schema = schema;
    }

    public void build(QueryStatement queryStatement, AggOption aggOption) throws Exception {
        this.metricQueryParam = queryStatement.getMetricQueryParam();
        if (metricQueryParam.getMetrics() == null) {
            metricQueryParam.setMetrics(new ArrayList<>());
        }
        if (metricQueryParam.getDimensions() == null) {
            metricQueryParam.setDimensions(new ArrayList<>());
        }
        if (metricQueryParam.getLimit() == null) {
            metricQueryParam.setLimit(0L);
        }
        this.aggOption = aggOption;

        buildParseNode();
        Database database = queryStatement.getOntology().getDatabase();
        EngineType engineType = EngineType.fromString(database.getType());
        optimizeParseNode(engineType);
        String sql = getSql(engineType);

        queryStatement.setSql(sql);
        if (Objects.nonNull(queryStatement.getEnableOptimize())
                && queryStatement.getEnableOptimize()
                && Objects.nonNull(queryStatement.getDataSetAlias())
                && !queryStatement.getDataSetAlias().isEmpty()) {
            // simplify model sql with query sql
            String simplifySql = rewrite(getSqlByDataSet(engineType, sql,
                    queryStatement.getDataSetSql(), queryStatement.getDataSetAlias()), engineType);
            if (Objects.nonNull(simplifySql) && !simplifySql.isEmpty()) {
                log.debug("simplifySql [{}]", simplifySql);
                queryStatement.setDataSetSimplifySql(simplifySql);
            }
        }
    }

    private void buildParseNode() throws Exception {
        // find the match Datasource
        scope = SchemaBuilder.getScope(schema);
        List<DataModel> dataModels =
                DataModelNode.getRelatedDataModels(scope, schema, metricQueryParam);
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
                previous.render(metricQueryParam, dataModels, scope, schema, !isAgg);
                renderer.setTable(previous
                        .builderAs(DataModelNode.getNames(dataModels) + "_" + String.valueOf(i)));
                i++;
            }
            previous = renderer;
        }
        builders.getLast().render(metricQueryParam, dataModels, scope, schema, !isAgg);
        parserNode = builders.getLast().builder();
    }

    private boolean getAgg(DataModel dataModel) {
        if (!AggOption.DEFAULT.equals(aggOption)) {
            return AggOption.isAgg(aggOption);
        }
        // default by dataModel time aggregation
        if (Objects.nonNull(dataModel.getAggTime()) && !dataModel.getAggTime()
                .equalsIgnoreCase(Constants.DIMENSION_TYPE_TIME_GRANULARITY_NONE)) {
            if (!metricQueryParam.isNativeQuery()) {
                return true;
            }
        }
        return isAgg;
    }

    public String getSql(EngineType engineType) {
        return SemanticNode.getSql(parserNode, engineType);
    }

    private String rewrite(String sql, EngineType engineType) {
        try {
            SqlNode sqlNode =
                    SqlParser.create(sql, Configuration.getParserConfig(engineType)).parseStmt();
            if (Objects.nonNull(sqlNode)) {
                return SemanticNode.getSql(
                        SemanticNode.optimize(scope, schema, sqlNode, engineType), engineType);
            }
        } catch (Exception e) {
            log.error("optimize error {}", e.toString());
        }
        return "";
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

    private String getSqlByDataSet(EngineType engineType, String parentSql, String dataSetSql,
            String parentAlias) throws SqlParseException {
        if (!SqlMergeWithUtils.hasWith(engineType, dataSetSql)) {
            return String.format("with %s as (%s) %s", parentAlias, parentSql, dataSetSql);
        }
        return SqlMergeWithUtils.mergeWith(engineType, dataSetSql,
                Collections.singletonList(parentSql), Collections.singletonList(parentAlias));
    }

}
