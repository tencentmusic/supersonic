package com.tencent.supersonic.headless.core.parser.calcite.planner;


import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.core.parser.calcite.Configuration;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SchemaBuilder;
import com.tencent.supersonic.headless.core.parser.calcite.sql.Renderer;
import com.tencent.supersonic.headless.core.parser.calcite.sql.TableView;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.DataSourceNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.SemanticNode;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.parser.calcite.schema.HeadlessSchema;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSqlDialect;
import com.tencent.supersonic.headless.core.parser.calcite.sql.optimizer.FilterToGroupScanRule;
import com.tencent.supersonic.headless.core.parser.calcite.sql.render.FilterRender;
import com.tencent.supersonic.headless.core.parser.calcite.sql.render.OutputRender;
import com.tencent.supersonic.headless.core.parser.calcite.sql.render.SourceRender;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Stack;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.calcite.sql2rel.SqlToRelConverter;

@Slf4j
public class AggPlanner implements Planner {

    private MetricQueryReq metricReq;
    private HeadlessSchema schema;
    private SqlValidatorScope scope;
    private Stack<TableView> dataSets = new Stack<>();
    private SqlNode parserNode;
    private String sourceId;
    private boolean isAgg = false;
    private AggOption aggOption = AggOption.DEFAULT;

    public AggPlanner(HeadlessSchema schema) {
        this.schema = schema;
    }

    public void parse() throws Exception {
        // find the match Datasource
        scope = SchemaBuilder.getScope(schema);
        List<DataSource> datasource = getMatchDataSource(scope);
        if (datasource == null || datasource.isEmpty()) {
            throw new Exception("datasource not found");
        }
        isAgg = getAgg(datasource.get(0));
        sourceId = String.valueOf(datasource.get(0).getSourceId());

        // build  level by level
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
                previous.render(metricReq, datasource, scope, schema, !isAgg);
                renderer.setTable(previous.builderAs(DataSourceNode.getNames(datasource) + "_" + String.valueOf(i)));
                i++;
            }
            previous = renderer;
        }
        builders.getLast().render(metricReq, datasource, scope, schema, !isAgg);
        parserNode = builders.getLast().builder();


    }

    private List<DataSource> getMatchDataSource(SqlValidatorScope scope) throws Exception {
        return DataSourceNode.getMatchDataSources(scope, schema, metricReq);
    }

    private boolean getAgg(DataSource dataSource) {
        if (!AggOption.DEFAULT.equals(aggOption)) {
            return AggOption.isAgg(aggOption);
        }
        // default by dataSource time aggregation
        if (Objects.nonNull(dataSource.getAggTime()) && !dataSource.getAggTime().equalsIgnoreCase(
                Constants.DIMENSION_TYPE_TIME_GRANULARITY_NONE)) {
            if (!metricReq.isNativeQuery()) {
                return true;
            }
        }
        return isAgg;
    }

    @Override
    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception {
        this.metricReq = queryStatement.getMetricReq();
        if (metricReq.getMetrics() == null) {
            metricReq.setMetrics(new ArrayList<>());
        }
        if (metricReq.getDimensions() == null) {
            metricReq.setDimensions(new ArrayList<>());
        }
        if (metricReq.getLimit() == null) {
            metricReq.setLimit(0L);
        }
        this.aggOption = aggOption;
        // build a parse Node
        parse();
        // optimizer
        optimize();
    }

    public void optimize() {
        if (Objects.isNull(schema.getRuntimeOptions()) || Objects.isNull(schema.getRuntimeOptions().getEnableOptimize())
                || !schema.getRuntimeOptions().getEnableOptimize()) {
            return;
        }
        HepProgramBuilder hepProgramBuilder = new HepProgramBuilder();
        hepProgramBuilder.addRuleInstance(new FilterToGroupScanRule(FilterToGroupScanRule.DEFAULT, schema));
        RelOptPlanner relOptPlanner = new HepPlanner(hepProgramBuilder.build());
        RelToSqlConverter converter = new RelToSqlConverter(SemanticSqlDialect.DEFAULT);
        SqlValidator sqlValidator = Configuration.getSqlValidator(
                scope.getValidator().getCatalogReader().getRootSchema());
        try {
            log.info("before optimize {}", SemanticNode.getSql(parserNode));
            SqlToRelConverter sqlToRelConverter = Configuration.getSqlToRelConverter(scope, sqlValidator,
                    relOptPlanner);
            RelNode sqlRel = sqlToRelConverter.convertQuery(
                    sqlValidator.validate(parserNode), false, true).rel;
            log.debug("RelNode optimize {}", SemanticNode.getSql(converter.visitRoot(sqlRel).asStatement()));
            relOptPlanner.setRoot(sqlRel);
            RelNode relNode = relOptPlanner.findBestExp();
            parserNode = converter.visitRoot(relNode).asStatement();
            log.debug("after optimize {}", SemanticNode.getSql(parserNode));
        } catch (Exception e) {
            log.error("optimize error {}", e);
        }
    }

    @Override
    public String getSql() {
        return SemanticNode.getSql(parserNode);
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public HeadlessSchema findBest() {
        return schema;
    }
}
