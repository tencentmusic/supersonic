package com.tencent.supersonic.semantic.query.parser.calcite.planner;

import com.tencent.supersonic.common.util.calcite.SqlParseUtils;
import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.query.parser.calcite.Configuration;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Constants;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Identify;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Materialization;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Materialization.TimePartType;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.MaterializationElement;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Measure;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.TimeRange;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SchemaBuilder;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.RelBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.util.CollectionUtils;

@Slf4j
public class MaterializationPlanner implements Planner {

    protected SemanticSchema schema;
    protected CalciteSchema viewSchema;
    protected HepProgramBuilder hepProgramBuilder;
    protected RelOptPlanner relOptPlanner;
    protected RelBuilder relBuilder;
    protected CalciteCatalogReader calciteCatalogReader;

    protected Comparator materializationSort = new Comparator<Entry<Long, Set<String>>>() {
        @Override
        public int compare(Entry<Long, Set<String>> o1, Entry<Long, Set<String>> o2) {
            if (o1.getValue().size() == o2.getValue().size()) {
                Optional<Materialization> o1Lever = schema.getMaterializationList().stream()
                        .filter(m -> m.getMaterializationId().equals(o1.getKey())).findFirst();
                Optional<Materialization> o2Lever = schema.getMaterializationList().stream()
                        .filter(m -> m.getMaterializationId().equals(o2.getKey())).findFirst();
                if (o1Lever.isPresent() && o2Lever.isPresent()) {
                    return o2Lever.get().getLevel() - o1Lever.get().getLevel();
                }
                return 0;
            }
            return o2.getValue().size() - o1.getValue().size();
        }
    };

    public MaterializationPlanner(SemanticSchema schema) {
        this.schema = schema;
        init();
    }

    @Override
    public void explain(QueryStatement queryStatement, AggOption isAgg) throws Exception {
        // findMatchMaterialization
        // checkValid  field + time
        if (CollectionUtils.isEmpty(queryStatement.getTimeRanges())) {
            //has no matchMaterialization time info
            return;
        }
        Set<String> fields = new HashSet<>();
        MetricReq metricCommand = queryStatement.getMetricReq();
        if (!Objects.isNull(metricCommand.getWhere()) && !metricCommand.getWhere().isEmpty()) {
            fields.addAll(SqlParseUtils.getFilterField(metricCommand.getWhere()));
        }
        if (!CollectionUtils.isEmpty(metricCommand.getMetrics())) {
            fields.addAll(metricCommand.getMetrics());
        }
        if (!CollectionUtils.isEmpty(metricCommand.getDimensions())) {
            fields.addAll(metricCommand.getDimensions());
        }
        Map<Long, Set<String>> matchMaterialization = new HashMap<>();
        Map<Long, Long> materializationDataBase = schema.getMaterializationList().stream()
                .collect(Collectors.toMap(Materialization::getMaterializationId, Materialization::getDataBase));
        for (String elem : fields) {
            boolean checkOk = false;
            for (Materialization materialization : schema.getMaterializationList()) {
                if (check(metricCommand, materialization, elem, queryStatement.getTimeRanges())) {
                    if (!matchMaterialization.containsKey(materialization.getMaterializationId())) {
                        matchMaterialization.put(materialization.getMaterializationId(), new HashSet<>());
                    }
                    matchMaterialization.get(materialization.getMaterializationId()).add(elem);
                    checkOk = true;
                }
            }
            if (!checkOk) {
                log.info("check fail [{}]", elem);
            }
        }
        if (!CollectionUtils.isEmpty(matchMaterialization)) {
            List<Entry<Long, Set<String>>> sortedMaterialization = new ArrayList<>(matchMaterialization.entrySet());
            sortedMaterialization.stream().collect(Collectors.toList()).sort(materializationSort);
            for (Entry<Long, Set<String>> m : sortedMaterialization) {
                Optional<Materialization> materialization = schema.getMaterializationList().stream()
                        .filter(mz -> mz.getMaterializationId().equals(m.getKey())).findFirst();
                if (!materialization.isPresent()) {
                    continue;
                }
                Set<String> viewField = new HashSet<>(m.getValue());
                viewField.add(materialization.get().getEntities());
                viewField.add(materialization.get().getDateInfo());
                if (materialization.get().getTimePartType().equals(TimePartType.ZIPPER)) {
                    viewField.add(Constants.MATERIALIZATION_ZIPPER_START + materialization.get().getDateInfo());
                    viewField.add(Constants.MATERIALIZATION_ZIPPER_END + materialization.get().getDateInfo());
                }
                if (viewField.containsAll(fields)) {
                    addDataSource(materialization.get());
                    break;
                }
                List<Entry<Long, Set<String>>> linkMaterialization = new ArrayList<>();
                for (Entry<Long, Set<String>> mm : sortedMaterialization) {
                    if (mm.getKey().equals(m.getKey())) {
                        continue;
                    }
                    if (materializationDataBase.get(mm.getKey()).equals(materializationDataBase.get(m.getKey()))) {
                        linkMaterialization.add(mm);
                    }
                }
                if (!CollectionUtils.isEmpty(linkMaterialization)) {
                    linkMaterialization.sort(materializationSort);
                    for (Entry<Long, Set<String>> mm : linkMaterialization) {
                        Set<String> linkField = new HashSet<>(mm.getValue());
                        linkField.addAll(viewField);
                        if (linkField.containsAll(fields)) {
                            Optional<Materialization> linkMaterial = schema.getMaterializationList().stream()
                                    .filter(mz -> mz.getMaterializationId().equals(mm.getKey())).findFirst();
                            if (linkMaterial.isPresent()) {
                                addDataSource(materialization.get());
                                addDataSource(linkMaterial.get());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void addDataSource(Materialization materialization) {
        Identify identify = new Identify();
        identify.setName(materialization.getEntities());
        List<Measure> metrics = materialization.getMetrics().stream()
                .map(m -> Measure.builder().name(m.getName()).expr(m.getName()).build()).collect(
                        Collectors.toList());
        List<Dimension> dimensions = materialization.getDimensions().stream()
                .map(d -> Dimension.builder().name(d.getName()).expr(d.getName()).build()).collect(
                        Collectors.toList());
        if (materialization.getTimePartType().equals(TimePartType.ZIPPER)) {
            dimensions.add(
                    Dimension.builder().name(Constants.MATERIALIZATION_ZIPPER_START + materialization.getDateInfo())
                            .type(Constants.DIMENSION_TYPE_TIME)
                            .expr(Constants.MATERIALIZATION_ZIPPER_START + materialization.getDateInfo()).build());
            dimensions.add(
                    Dimension.builder().name(Constants.MATERIALIZATION_ZIPPER_END + materialization.getDateInfo())
                            .type(Constants.DIMENSION_TYPE_TIME)
                            .expr(Constants.MATERIALIZATION_ZIPPER_END + materialization.getDateInfo()).build());
        } else {
            dimensions.add(Dimension.builder().name(materialization.getDateInfo()).expr(materialization.getDateInfo())
                    .type(Constants.DIMENSION_TYPE_TIME)
                    .build());
        }

        DataSource dataSource = DataSource.builder().sourceId(materialization.getDataBase())
                .tableQuery(materialization.getDestinationTable())
                .timePartType(materialization.getTimePartType())
                .name("v_" + String.valueOf(materialization.getMaterializationId()))
                .identifiers(Arrays.asList(identify))
                .measures(metrics)
                .dimensions(dimensions)
                .build();
        schema.getDatasource().put(dataSource.getName(), dataSource);
    }

    @Override
    public String getSql() {
        return null;
    }

    @Override
    public String getSourceId() {
        return null;
    }

    @Override
    public SemanticSchema findBest() {
        return schema;
    }


    private void init() {
        viewSchema = SchemaBuilder.getMaterializationSchema();
        hepProgramBuilder = new HepProgramBuilder();
        hepProgramBuilder.addRuleInstance(MaterializedViewRules.PROJECT_FILTER);
        relOptPlanner = new HepPlanner(hepProgramBuilder.build());
        calciteCatalogReader = new CalciteCatalogReader(
                CalciteSchema.from(viewSchema.plus()),
                CalciteSchema.from(viewSchema.plus()).path(null),
                Configuration.typeFactory,
                new CalciteConnectionConfigImpl(new Properties()));

        relOptPlanner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        relOptPlanner.addRelTraitDef(RelDistributionTraitDef.INSTANCE);
        EnumerableRules.rules().forEach(relOptPlanner::addRule);

        RexBuilder rexBuilder = new RexBuilder(Configuration.typeFactory);
        RelOptCluster relOptCluster = RelOptCluster.create(relOptPlanner, rexBuilder);
        relBuilder = RelFactories.LOGICAL_BUILDER.create(relOptCluster, calciteCatalogReader);
    }

    private RexNode getRexNode(List<ImmutablePair<String, String>> timeRanges, String viewField) {
        RexNode rexNode = null;
        for (ImmutablePair<String, String> timeRange : timeRanges) {
            if (rexNode == null) {
                rexNode = getRexNodeByTimeRange(TimeRange.builder().start(timeRange.left).end(timeRange.right).build(),
                        viewField);
                continue;
            }
            rexNode = relBuilder.call(SqlStdOperatorTable.OR, rexNode,
                    getRexNodeByTimeRange(TimeRange.builder().start(timeRange.left).end(timeRange.right).build(),
                            viewField));
        }
        return rexNode;
    }

    private RexNode getRexNode(Materialization materialization, String elem, String viewField) {
        Optional<MaterializationElement> dim = materialization.getDimensions()
                .stream().filter(d -> d.getName().equalsIgnoreCase(elem)).findFirst();
        if (!dim.isPresent()) {
            dim = materialization.getMetrics().stream().filter(m -> m.getName().equalsIgnoreCase(elem)).findFirst();
        }
        RexNode rexNode = null;
        if (dim.isPresent()) {
            for (TimeRange timeRange : dim.get().getTimeRangeList()) {
                if (rexNode == null) {
                    rexNode = getRexNodeByTimeRange(timeRange, viewField);
                    continue;
                }
                rexNode = relBuilder.call(SqlStdOperatorTable.OR, rexNode, getRexNodeByTimeRange(timeRange, viewField));
            }
        }
        return rexNode;
    }

    private RexNode getRexNodeByTimeRange(TimeRange timeRange, String field) {
        return relBuilder.call(SqlStdOperatorTable.AND,
                relBuilder.call(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL, relBuilder.field(field),
                        relBuilder.literal(timeRange.getStart())),
                relBuilder.call(SqlStdOperatorTable.LESS_THAN_OR_EQUAL, relBuilder.field(field),
                        relBuilder.literal(timeRange.getEnd())));
    }

    public boolean check(MetricReq metricCommand, Materialization materialization, String elem,
                         List<ImmutablePair<String, String>> timeRanges)
            throws SqlParseException {
        boolean isMatch = false;
        try {
            relBuilder.clear();
            if (!CollectionUtils.isEmpty(relOptPlanner.getMaterializations())) {
                relOptPlanner.clear();
            }
            String db = SchemaBuilder.MATERIALIZATION_SYS_DB;
            RelBuilder viewBuilder = relBuilder.scan(Arrays.asList(db, SchemaBuilder.MATERIALIZATION_SYS_SOURCE));
            RexNode viewFilter = getRexNode(materialization, elem, SchemaBuilder.MATERIALIZATION_SYS_FIELD_DATE);
            if (viewFilter == null) {
                return false;
            }
            RelNode viewRel = viewBuilder.filter(viewFilter).project(relBuilder.fields()).build();
            log.debug("view {}", viewRel.explain());
            List<String> view = Arrays.asList(db, SchemaBuilder.MATERIALIZATION_SYS_VIEW);
            RelNode replacement = relBuilder.scan(view).build();
            RelOptMaterialization relOptMaterialization = new RelOptMaterialization(replacement, viewRel, null, view);
            relOptPlanner.addMaterialization(relOptMaterialization);

            RelNode checkRel = relBuilder.scan(Arrays.asList(db, SchemaBuilder.MATERIALIZATION_SYS_SOURCE))
                    .filter(getRexNode(timeRanges, SchemaBuilder.MATERIALIZATION_SYS_FIELD_DATE))
                    .project(relBuilder.field(SchemaBuilder.MATERIALIZATION_SYS_FIELD_DATE)).build();

            relOptPlanner.setRoot(checkRel);
            RelNode optRel = relOptPlanner.findBestExp();
            log.debug("findBestExp {}", optRel.explain());
            isMatch = !extractTableNames(optRel).contains(SchemaBuilder.MATERIALIZATION_SYS_SOURCE);
        } catch (Exception e) {
            log.error("check error {}", e);
        }
        return isMatch;
    }

    public static Set<String> extractTableNames(RelNode relNode) {
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


}
