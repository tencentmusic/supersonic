package com.tencent.supersonic.headless.core.parser.calcite.sql.optimizer;

import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSchema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.rules.FilterTableScanRule;
import org.apache.calcite.rel.rules.FilterTableScanRule.Config;
import org.apache.calcite.rel.rules.TransformationRule;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 *  push down the time filter into group using the RuntimeOptions defined minMaxTime
 *
 */
public class FilterToGroupScanRule extends RelRule<Config>
        implements TransformationRule {

    public static FilterTableScanRule.Config DEFAULT = FilterTableScanRule.Config.DEFAULT.withOperandSupplier((b0) -> {
        return b0.operand(LogicalFilter.class).oneInput((b1) -> {
            return b1.operand(LogicalProject.class).oneInput((b2) -> {
                return b2.operand(LogicalAggregate.class).oneInput((b3) -> {
                    return b3.operand(LogicalProject.class).anyInputs();
                });
            });
        });

    }).as(FilterTableScanRule.Config.class);

    private SemanticSchema semanticSchema;

    public FilterToGroupScanRule(FilterTableScanRule.Config config, SemanticSchema semanticSchema) {
        super(config);
        this.semanticSchema = semanticSchema;
    }

    public void onMatch(RelOptRuleCall call) {
        if (call.rels.length != 4) {
            return;
        }
        if (Objects.isNull(semanticSchema.getRuntimeOptions()) || Objects.isNull(
                semanticSchema.getRuntimeOptions().getMinMaxTime()) || semanticSchema.getRuntimeOptions()
                .getMinMaxTime().getLeft().isEmpty()) {
            return;
        }
        Triple<String, String, String> minMax = semanticSchema.getRuntimeOptions().getMinMaxTime();
        Filter filter = (Filter) call.rel(0);
        Project project0 = (Project) call.rel(1);
        Project project1 = (Project) call.rel(3);
        Aggregate logicalAggregate = (Aggregate) call.rel(2);
        Optional<Pair<RexNode, String>> isIn = project1.getNamedProjects()
                .stream().filter(i -> i.right.equalsIgnoreCase(minMax.getLeft())).findFirst();
        if (!isIn.isPresent()) {
            return;
        }

        RelBuilder relBuilder = call.builder();
        relBuilder.push(project1);
        RexNode addPartitionCondition = getRexNodeByTimeRange(relBuilder, minMax.getLeft(), minMax.getMiddle(),
                minMax.getRight());
        relBuilder.filter(new RexNode[]{addPartitionCondition});
        relBuilder.project(project1.getProjects());
        ImmutableBitSet newGroupSet = logicalAggregate.getGroupSet();
        int newGroupCount = newGroupSet.cardinality();
        int groupCount = logicalAggregate.getGroupCount();
        List<AggregateCall> newAggCalls = new ArrayList();
        Iterator var = logicalAggregate.getAggCallList().iterator();
        while (var.hasNext()) {
            AggregateCall aggCall = (AggregateCall) var.next();
            newAggCalls.add(
                    aggCall.adaptTo(project1, aggCall.getArgList(), aggCall.filterArg, groupCount, newGroupCount));
        }
        relBuilder.aggregate(relBuilder.groupKey(newGroupSet), newAggCalls);
        relBuilder.project(project0.getProjects());
        relBuilder.filter(new RexNode[]{filter.getCondition()});
        call.transformTo(relBuilder.build());
    }

    private RexNode getRexNodeByTimeRange(RelBuilder relBuilder, String dateField, String start, String end) {
        return relBuilder.call(SqlStdOperatorTable.AND,
                relBuilder.call(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL, relBuilder.field(dateField),
                        relBuilder.literal(start)),
                relBuilder.call(SqlStdOperatorTable.LESS_THAN_OR_EQUAL, relBuilder.field(dateField),
                        relBuilder.literal(end)));
    }

}
