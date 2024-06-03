package com.tencent.supersonic.headless.core.executor.accelerator;

import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.core.parser.calcite.Configuration;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.TimeRange;
import com.tencent.supersonic.headless.core.parser.calcite.schema.DataSourceTable;
import com.tencent.supersonic.headless.core.parser.calcite.schema.DataSourceTable.Builder;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SchemaBuilder;
import com.tencent.supersonic.headless.core.pojo.Materialization;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.adapter.enumerable.EnumerableRules;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptMaterialization;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelDistributionTraitDef;
import org.apache.calcite.rel.RelHomogeneousShuttle;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttle;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.rules.materialize.MaterializedViewRules;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.RelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.util.CollectionUtils;

/**
 * abstract of accelerator , provide Basic methods
 */
@Slf4j
public abstract class AbstractAccelerator implements QueryAccelerator {

    public static final String MATERIALIZATION_SYS_DB = "sys";
    public static final String MATERIALIZATION_SYS_SOURCE = "sys_src";
    public static final String MATERIALIZATION_SYS_VIEW = "sys_view";
    public static final String MATERIALIZATION_SYS_PARTITION = "sys_partition";

    /**
     * check if a materialization match the fields and partitions
     */
    protected boolean check(RelOptPlanner relOptPlanner, RelBuilder relBuilder,
            CalciteCatalogReader calciteCatalogReader, Materialization materialization, List<String> fields,
            List<ImmutablePair<String, String>> partitions) {
        if (!materialization.isPartitioned()) {
            return fields.stream().allMatch(f -> materialization.getColumns().contains(f));
        }
        Set<String> queryFields = new HashSet<>(fields);
        queryFields.add(MATERIALIZATION_SYS_PARTITION);
        List<String> queryFieldList = queryFields.stream().collect(Collectors.toList());

        Set<String> viewFields = new HashSet<>(materialization.getColumns());
        viewFields.add(MATERIALIZATION_SYS_PARTITION);
        List<String> viewFieldList = viewFields.stream().collect(Collectors.toList());

        Set<String> materializationFields = new HashSet<>(viewFields);
        materializationFields.addAll(queryFields);
        List<String> materializationFieldList = materializationFields.stream().collect(Collectors.toList());

        relBuilder.clear();
        if (!CollectionUtils.isEmpty(relOptPlanner.getMaterializations())) {
            relOptPlanner.clear();
        }

        Materialization viewMaterialization = Materialization.builder().build();
        viewMaterialization.setName(String.format("%s.%s", MATERIALIZATION_SYS_DB, MATERIALIZATION_SYS_VIEW));
        viewMaterialization.setColumns(viewFieldList);
        addMaterialization(calciteCatalogReader.getRootSchema(), viewMaterialization);

        Materialization queryMaterialization = Materialization.builder().build();
        queryMaterialization.setName(String.format("%s.%s", MATERIALIZATION_SYS_DB, MATERIALIZATION_SYS_SOURCE));

        queryMaterialization.setColumns(materializationFieldList);
        addMaterialization(calciteCatalogReader.getRootSchema(), queryMaterialization);

        RelNode replacement = relBuilder.scan(Arrays.asList(MATERIALIZATION_SYS_DB, MATERIALIZATION_SYS_VIEW)).build();
        RelBuilder viewBuilder = relBuilder.scan(Arrays.asList(MATERIALIZATION_SYS_DB, MATERIALIZATION_SYS_SOURCE));
        if (materialization.isPartitioned()) {
            RexNode viewFilter = getRexNode(relBuilder, materialization,
                    MATERIALIZATION_SYS_PARTITION);
            viewBuilder = viewBuilder.filter(viewFilter);
        }
        RelNode viewRel = project(viewBuilder, viewFieldList).build();
        List<String> view = Arrays.asList(MATERIALIZATION_SYS_DB, MATERIALIZATION_SYS_VIEW);
        RelOptMaterialization relOptMaterialization = new RelOptMaterialization(replacement, viewRel, null,
                view);
        relOptPlanner.addMaterialization(relOptMaterialization);

        RelBuilder checkBuilder = relBuilder.scan(Arrays.asList(MATERIALIZATION_SYS_DB, MATERIALIZATION_SYS_SOURCE));
        if (materialization.isPartitioned()) {
            checkBuilder = checkBuilder.filter(getRexNode(checkBuilder, partitions, MATERIALIZATION_SYS_PARTITION));
        }
        RelNode checkRel = project(checkBuilder, queryFieldList).build();
        relOptPlanner.setRoot(checkRel);
        RelNode optRel = relOptPlanner.findBestExp();
        System.out.println(optRel.explain());
        return !extractTableNames(optRel).contains(MATERIALIZATION_SYS_SOURCE);
    }

    protected Map<String, Set<String>> getFields(String sql) {
        return SqlSelectHelper.getFieldsWithSubQuery(sql);
    }

    protected CalciteCatalogReader getCalciteCatalogReader() {
        CalciteCatalogReader calciteCatalogReader;
        CalciteSchema viewSchema = SchemaBuilder.getMaterializationSchema();
        calciteCatalogReader = new CalciteCatalogReader(
                CalciteSchema.from(viewSchema.plus()),
                CalciteSchema.from(viewSchema.plus()).path(null),
                Configuration.typeFactory,
                new CalciteConnectionConfigImpl(new Properties()));
        return calciteCatalogReader;
    }

    protected RelOptPlanner getRelOptPlanner() {
        HepProgramBuilder hepProgramBuilder = new HepProgramBuilder();
        hepProgramBuilder.addRuleInstance(MaterializedViewRules.PROJECT_FILTER);
        RelOptPlanner relOptPlanner = new HepPlanner(hepProgramBuilder.build());
        return relOptPlanner;
    }

    protected RelBuilder builderMaterializationPlan(CalciteCatalogReader calciteCatalogReader,
            RelOptPlanner relOptPlanner) {
        relOptPlanner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        relOptPlanner.addRelTraitDef(RelDistributionTraitDef.INSTANCE);
        EnumerableRules.rules().forEach(relOptPlanner::addRule);
        RexBuilder rexBuilder = new RexBuilder(Configuration.typeFactory);
        RelOptCluster relOptCluster = RelOptCluster.create(relOptPlanner, rexBuilder);
        return RelFactories.LOGICAL_BUILDER.create(relOptCluster, calciteCatalogReader);
    }

    protected void addMaterialization(CalciteSchema dataSetSchema, Materialization materialization) {
        String[] dbTable = materialization.getName().split("\\.");
        String tb = dbTable[1].toLowerCase();
        String db = dbTable[0].toLowerCase();
        Builder builder = DataSourceTable.newBuilder(tb);
        for (String f : materialization.getColumns()) {
            builder.addField(f, SqlTypeName.VARCHAR);
        }
        if (StringUtils.isNotBlank(materialization.getPartitionName())) {
            builder.addField(materialization.getPartitionName(), SqlTypeName.VARCHAR);
        }
        DataSourceTable srcTable = builder.withRowCount(1L).build();
        if (Objects.nonNull(db) && !db.isEmpty()) {
            SchemaPlus schemaPlus = dataSetSchema.plus().getSubSchema(db);
            if (Objects.isNull(schemaPlus)) {
                dataSetSchema.plus().add(db, new AbstractSchema());
                schemaPlus = dataSetSchema.plus().getSubSchema(db);
            }
            schemaPlus.add(tb, srcTable);
        } else {
            dataSetSchema.add(tb, srcTable);
        }

    }

    protected Set<String> extractTableNames(RelNode relNode) {
        Set<String> tableNames = new HashSet<>();
        RelShuttle shuttle = new RelHomogeneousShuttle() {
            public RelNode visit(TableScan scan) {
                RelOptTable table = scan.getTable();
                tableNames.addAll(table.getQualifiedName());
                return scan;
            }
        };
        relNode.accept(shuttle);
        return tableNames;
    }

    protected RexNode getRexNodeByTimeRange(RelBuilder relBuilder, TimeRange timeRange, String field) {
        return relBuilder.call(SqlStdOperatorTable.AND,
                relBuilder.call(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL, relBuilder.field(field),
                        relBuilder.literal(timeRange.getStart())),
                relBuilder.call(SqlStdOperatorTable.LESS_THAN_OR_EQUAL, relBuilder.field(field),
                        relBuilder.literal(timeRange.getEnd())));
    }

    protected RexNode getRexNode(RelBuilder relBuilder, Materialization materialization, String viewField) {
        RexNode rexNode = null;
        for (String partition : materialization.getPartitions()) {
            TimeRange timeRange = TimeRange.builder().start(partition).end(partition).build();
            if (rexNode == null) {
                rexNode = getRexNodeByTimeRange(relBuilder, timeRange, viewField);
                continue;
            }
            rexNode = relBuilder.call(SqlStdOperatorTable.OR, rexNode,
                    getRexNodeByTimeRange(relBuilder, timeRange, viewField));
        }
        return rexNode;
    }

    protected RexNode getRexNode(RelBuilder relBuilder, List<ImmutablePair<String, String>> timeRanges,
            String viewField) {
        RexNode rexNode = null;
        for (ImmutablePair<String, String> timeRange : timeRanges) {
            if (rexNode == null) {
                rexNode = getRexNodeByTimeRange(relBuilder,
                        TimeRange.builder().start(timeRange.left).end(timeRange.right).build(),
                        viewField);
                continue;
            }
            rexNode = relBuilder.call(SqlStdOperatorTable.OR, rexNode,
                    getRexNodeByTimeRange(relBuilder,
                            TimeRange.builder().start(timeRange.left).end(timeRange.right).build(),
                            viewField));
        }
        return rexNode;
    }

    private static RelBuilder project(RelBuilder relBuilder, List<String> fields) {
        List<RexNode> rexNodes = fields.stream().map(f -> relBuilder.field(f)).collect(Collectors.toList());
        return relBuilder.project(rexNodes);
    }
}
