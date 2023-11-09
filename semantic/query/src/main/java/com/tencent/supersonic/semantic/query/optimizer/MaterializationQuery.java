package com.tencent.supersonic.semantic.query.optimizer;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.common.util.calcite.SqlParseUtils;
import com.tencent.supersonic.semantic.api.materialization.enums.MaterializedTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.api.model.enums.QueryOptMode;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.materialization.domain.MaterializationConfService;
import com.tencent.supersonic.semantic.materialization.domain.MaterializationRecordService;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.parser.calcite.SemanticSchemaManager;
import com.tencent.supersonic.semantic.query.parser.calcite.planner.AggPlanner;
import com.tencent.supersonic.semantic.query.parser.calcite.planner.MaterializationPlanner;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.DataType;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Materialization;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Materialization.TimePartType;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.MaterializationElement;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Measure;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Metric;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.TimeRange;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import com.tencent.supersonic.semantic.query.utils.StatUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component("MaterializationQuery")
public class MaterializationQuery implements QueryOptimizer {

    protected final MaterializationConfService materializationConfService;
    protected final MaterializationRecordService materializationRecordService;
    protected final Catalog catalog;
    protected final QueryStructUtils queryStructUtils;
    protected final SemanticSchemaManager semanticSchemaManager;
    protected final StatUtils statUtils;


    @Value("${materialization.query.enable:false}")
    private boolean enabled;

    public MaterializationQuery(
            MaterializationConfService materializationConfService,
            MaterializationRecordService materializationRecordService,
            Catalog catalog, QueryStructUtils queryStructUtils,
            SemanticSchemaManager semanticSchemaManager,
            StatUtils statUtils) {
        this.materializationConfService = materializationConfService;
        this.materializationRecordService = materializationRecordService;
        this.catalog = catalog;

        this.queryStructUtils = queryStructUtils;
        this.semanticSchemaManager = semanticSchemaManager;
        this.statUtils = statUtils;
    }

    @Override
    public void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement) {
        if (!enabled) {
            return;
        }
        try {
            if (Objects.isNull(queryStructCmd) || Objects.isNull(queryStatement) || Objects.isNull(
                    queryStructCmd.getModelId()) || Objects.isNull(
                    queryStructCmd.getDateInfo())) {
                return;
            }
            if (Objects.nonNull(queryStatement.getParseSqlReq())) {
                rewriteSqlReq(queryStructCmd, queryStatement);
                return;
            }
            List<Materialization> materializationList = getMaterializationSchema(queryStructCmd,
                    queryStatement.getMetricReq());
            if (!CollectionUtils.isEmpty(materializationList)) {
                if (replan(materializationList, queryStructCmd, queryStatement)) {
                    statUtils.updateQueryOptMode(QueryOptMode.MATERIALIZATION.name());
                }
            }
        } catch (Exception e) {
            log.error("MaterializationQuery error {}", e);
        }
    }

    protected void rewriteSqlReq(QueryStructReq queryStructCmd, QueryStatement queryStatement) throws Exception {
        ParseSqlReq parseSqlReq = queryStatement.getParseSqlReq();
        String sourceId = queryStatement.getSourceId();
        String sql = queryStatement.getSql();
        String parseSql = parseSqlReq.getSql();
        String msg = queryStatement.getErrMsg();
        String materializationSourceId = "";
        String materializationSql = "";
        String parseSqlReqMaterialization = "";
        if (!CollectionUtils.isEmpty(parseSqlReq.getTables())) {
            List<String[]> tables = new ArrayList<>();
            for (MetricTable metricTable : parseSqlReq.getTables()) {
                MetricReq metricReq = new MetricReq();
                metricReq.setMetrics(metricTable.getMetrics());
                metricReq.setDimensions(metricTable.getDimensions());
                metricReq.setWhere(StringUtil.formatSqlQuota(metricTable.getWhere()));
                metricReq.setRootPath(parseSqlReq.getRootPath());
                List<Materialization> materializationList = getMaterializationSchema(queryStructCmd,
                        metricReq);
                if (!CollectionUtils.isEmpty(materializationList)) {
                    queryStatement.setMetricReq(metricReq);
                    boolean ok = replan(materializationList, queryStructCmd, queryStatement);
                    if (!ok) {
                        log.info("MaterializationQuery rewriteSqlReq not match {}", metricTable.getAlias());
                        queryStatement.setSql(sql);
                        queryStatement.setSourceId(sourceId);
                        queryStatement.setErrMsg(msg);
                        queryStatement.setMetricReq(null);
                        return;
                    }
                    tables.add(new String[]{metricTable.getAlias(), queryStatement.getSql()});
                    materializationSourceId = queryStatement.getSourceId();
                    parseSqlReqMaterialization = queryStatement.getParseSqlReq().getSql();
                }
            }
            if (!CollectionUtils.isEmpty(tables)) {
                if (parseSqlReq.isSupportWith()) {
                    materializationSql = "with " + String.join(",",
                            tables.stream().map(t -> String.format("%s as (%s)", t[0], t[1])).collect(
                                    Collectors.toList())) + "\n" + parseSqlReqMaterialization;
                } else {
                    materializationSql = parseSqlReqMaterialization;
                    for (String[] tb : tables) {
                        materializationSql = StringUtils.replace(materializationSql, tb[0],
                                "(" + tb[1] + ") " + (parseSqlReq.isWithAlias() ? "" : tb[0]), -1);
                    }
                }

                queryStatement.setSql(materializationSql);
                queryStatement.setSourceId(materializationSourceId);
                log.info("rewriteSqlReq before[{}]  after[{}]", sql, materializationSql);
                statUtils.updateQueryOptMode(QueryOptMode.MATERIALIZATION.name());
            }
            parseSqlReq.setSql(parseSql);
        }
    }

    public List<Materialization> getMaterializationSchema(QueryStructReq queryStructReq, MetricReq metricReq)
            throws Exception {
        List<Materialization> materializationList = new ArrayList<>();
        if (Objects.isNull(metricReq)) {
            return materializationList;
        }
        ImmutablePair<String, String> timeRange = queryStructUtils.getBeginEndTime(queryStructReq);
        String start = timeRange.left;
        String end = timeRange.right;
        Long modelId = queryStructReq.getModelId();
        List<MaterializationResp> materializationResps = materializationConfService.getMaterializationByModel(modelId);
        List<DimensionResp> dimensionResps = catalog.getDimensions(modelId);
        List<MetricResp> metrics = catalog.getMetrics(modelId);
        Set<String> fields = new HashSet<>();

        if (Objects.nonNull(metricReq.getWhere()) && !metricReq.getWhere().isEmpty()) {
            fields.addAll(SqlParseUtils.getFilterField(metricReq.getWhere()));
        }
        if (!CollectionUtils.isEmpty(metricReq.getMetrics())) {
            fields.addAll(metricReq.getMetrics());
        }
        if (!CollectionUtils.isEmpty(metricReq.getDimensions())) {
            fields.addAll(metricReq.getDimensions());
        }

        materializationResps.forEach(materializationResp -> {
            Materialization materialization =
                    Materialization.builder().dateInfo(materializationResp.getDateInfo())
                            .materializationId(materializationResp.getId())
                            .level(materializationResp.getLevel())
                            .timePartType(TimePartType.of(materializationResp.getMaterializedType().name()))
                            .dimensions(new ArrayList<>())
                            .metrics(new ArrayList<>())
                            .entities(materializationResp.getEntities())
                            .destinationTable(materializationResp.getDestinationTable()).modelId(modelId)
                            .dataBase(materializationResp.getDatabaseId()).build();
            List<Long> sameTableMaterialization = materializationConfService.getMaterializationByTable(
                    materializationResp.getDatabaseId(), materializationResp.getDestinationTable());
            Set<Long> metricIds = materializationResp.getMaterializationElementRespList().stream()
                    .filter(e -> e.getType().equals(
                            TypeEnums.METRIC)).map(e -> e.getId()).collect(Collectors.toSet());
            Set<Long> dimensionIds = materializationResp.getMaterializationElementRespList().stream()
                    .filter(e -> e.getType().equals(
                            TypeEnums.DIMENSION)).map(e -> e.getId()).collect(Collectors.toSet());

            dimensionResps.stream().filter(d -> dimensionIds.contains(d.getId()))
                    .filter(d -> fields.contains(d.getBizName())).forEach(d -> {
                        List<MaterializationRecordResp> materializationRecordResps = materializationRecordService
                                .fetchMaterializationDate(sameTableMaterialization, d.getBizName(), start, end);
                        if (!CollectionUtils.isEmpty(materializationRecordResps)) {
                            List<TimeRange> timeRangeList = new ArrayList<>();
                            materializationRecordResps.stream().forEach(t -> timeRangeList.add(
                                    TimeRange.builder().start(t.getDataTime()).end(t.getDataTime()).build()));
                            materialization.getDimensions().add(
                                    MaterializationElement.builder()
                                            .name(d.getBizName())
                                            .timeRangeList(timeRangeList)
                                            .build()
                            );
                        } else if (MaterializedTypeEnum.FULL.equals(materializationResp.getMaterializedType())) {
                            materialization.getDimensions().add(
                                    MaterializationElement.builder()
                                            .name(d.getBizName())
                                            .timeRangeList(
                                                    Arrays.asList(TimeRange.builder().start(start).end(end).build()))
                                            .build()
                            );
                        }
                    });
            metrics.stream().filter(m -> metricIds.contains(m.getId())).filter(m -> fields.contains(m.getBizName()))
                    .forEach(m -> {
                        List<MaterializationRecordResp> materializationRecordResps = materializationRecordService
                                .fetchMaterializationDate(sameTableMaterialization, m.getBizName(), start, end);
                        if (!CollectionUtils.isEmpty(materializationRecordResps)) {
                            List<TimeRange> timeRangeList = new ArrayList<>();
                            materializationRecordResps.stream().forEach(t -> timeRangeList.add(
                                    TimeRange.builder().start(t.getDataTime()).end(t.getDataTime()).build()));
                            materialization.getMetrics().add(MaterializationElement.builder().name(m.getBizName())
                                    .timeRangeList(timeRangeList).build());
                        } else if (MaterializedTypeEnum.FULL.equals(materializationResp.getMaterializedType())) {
                            materialization.getMetrics().add(
                                    MaterializationElement.builder()
                                            .name(m.getBizName())
                                            .timeRangeList(
                                                    Arrays.asList(TimeRange.builder().start(start).end(end).build()))
                                            .build()
                            );
                        }
                    });
            materializationList.add(materialization);
        });
        return materializationList;
    }

    protected boolean replan(List<Materialization> materializationList, QueryStructReq queryStructReq,
            QueryStatement queryStatement)
            throws Exception {
        log.info("{}", materializationList);
        SemanticSchema schema = SemanticSchema.newBuilder(queryStatement.getMetricReq().getRootPath()).build();
        schema.setMaterializationList(materializationList);
        getTimeRanges(queryStructReq, queryStatement);
        removeDefaultMetric(queryStructReq, queryStatement.getMetricReq());
        MaterializationPlanner materializationPlanner = new MaterializationPlanner(schema);
        materializationPlanner.explain(queryStatement, AggOption.getAggregation(queryStructReq.getNativeQuery()));
        log.info("optimize {}", materializationPlanner.findBest().getDatasource());
        SemanticSchema semanticSchema = materializationPlanner.findBest();
        if (!CollectionUtils.isEmpty(semanticSchema.getDatasource())) {
            semanticSchema.getSemanticModel().setRootPath(semanticSchema.getRootPath());
            semanticSchema.setSemanticModel(transform(queryStatement, semanticSchema.getSemanticModel()));
            int materCnt = semanticSchema.getDatasource().size();
            if (materCnt == semanticSchema.getDatasource().entrySet().stream()
                    .filter(d -> d.getValue().getTimePartType().equals(TimePartType.ZIPPER)).count()) {
                doSingleZipperSource(queryStructReq, queryStatement);
            }
            AggPlanner aggBuilder = new AggPlanner(semanticSchema);
            aggBuilder.explain(queryStatement, AggOption.getAggregation(queryStructReq.getNativeQuery()));
            log.debug("optimize before {} sql {}", queryStatement.getSourceId(), queryStatement.getSql());
            log.debug("optimize after {} sql {}", aggBuilder.getSourceId(), aggBuilder.getSql());
            queryStatement.setSourceId(aggBuilder.getSourceId());
            queryStatement.setSql(aggBuilder.getSql());
            queryStatement.setStatus(queryStatement.getStatus() + 1);
            return true;
        }
        return false;
    }

    protected SemanticModel transform(QueryStatement queryStatement, SemanticModel semanticModel) throws Exception {
        for (DataSource dataSource : semanticModel.getDatasourceMap().values()) {
            if (!CollectionUtils.isEmpty(dataSource.getMeasures())) {
                dataSource.getMeasures().stream().forEach(m -> {
                    setMetricExpr(semanticModel.getRootPath(), m.getName(), m);
                });
            }
            if (!CollectionUtils.isEmpty(dataSource.getDimensions())) {
                dataSource.getDimensions().stream().forEach(d -> {
                    setDimension(semanticModel.getRootPath(), d.getName(), d);
                });
            }
        }
        return semanticModel;
    }

    protected void setDimension(String rootPath, String bizName, Dimension dimension) {
        try {
            dimension.setDataType(DataType.UNKNOWN);
            SemanticModel oriSemanticModel = semanticSchemaManager.get(rootPath);
            if (Objects.nonNull(oriSemanticModel)) {
                for (List<Dimension> dimensions : oriSemanticModel.getDimensionMap().values()) {
                    Optional<Dimension> dim = dimensions.stream()
                            .filter(d -> d.getName().equalsIgnoreCase(bizName)).findFirst();
                    if (dim.isPresent()) {
                        dimension.setDataType(dim.get().getDataType());
                    }
                }
            }
        } catch (Exception e) {
            log.error("getMetricExpr {}", e);
        }
    }

    protected void setMetricExpr(String rootPath, String bizName, Measure measure) {
        try {
            measure.setExpr(bizName);
            SemanticModel oriSemanticModel = semanticSchemaManager.get(rootPath);
            if (Objects.nonNull(oriSemanticModel)) {
                Optional<Metric> metric = oriSemanticModel.getMetrics()
                        .stream().filter(m -> m.getName().equalsIgnoreCase(bizName)).findFirst();
                if (metric.isPresent()) {
                    if (metric.get().getMetricTypeParams().getExpr().contains(getVariablePrefix())) {
                        measure.setExpr(metric.get().getMetricTypeParams().getExpr());
                    }
                    if (!CollectionUtils.isEmpty(metric.get().getMetricTypeParams().getMeasures())) {
                        String measureParam = metric.get().getMetricTypeParams().getMeasures().get(0).getName();
                        for (DataSource dataSource : oriSemanticModel.getDatasourceMap().values()) {
                            Optional<Measure> measureOpt = dataSource.getMeasures().stream()
                                    .filter(mm -> mm.getName().equalsIgnoreCase(measureParam)).findFirst();
                            if (measureOpt.isPresent()) {
                                measure.setAgg(measureOpt.get().getAgg());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getMetricExpr {}", e);
        }
    }

    protected void removeDefaultMetric(QueryStructReq queryStructReq, MetricReq metricReq) {
        // due to default metrics have no materialization
        if (CollectionUtils.isEmpty(queryStructReq.getAggregators()) && Objects.nonNull(metricReq)) {
            metricReq.setMetrics(new ArrayList<>());
        }
    }

    protected void doSingleZipperSource(QueryStructReq queryStructReq, QueryStatement queryStatement) {
        // time field rewrite to start_ end_
        log.info("doSingleZipperSource {}", queryStatement);

        if (CollectionUtils.isEmpty(queryStructReq.getAggregators()) && CollectionUtils.isEmpty(
                queryStructReq.getGroups()) && Objects.nonNull(queryStatement.getParseSqlReq())) {
            String sqlNew = queryStructUtils.generateZipperWhere(queryStatement, queryStructReq);
            log.info("doSingleZipperSource before[{}] after[{}]", queryStatement.getParseSqlReq().getSql(), sqlNew);
            queryStatement.getParseSqlReq().setSql(sqlNew);
            return;
        }
        MetricReq metricReq = queryStatement.getMetricReq();
        String where = queryStructUtils.generateZipperWhere(queryStructReq);
        if (!where.isEmpty() && Objects.nonNull(metricReq)) {
            log.info("doSingleZipperSource before[{}] after[{}]", metricReq.getWhere(), where);
            metricReq.setWhere(where);
        }
    }

    protected void getTimeRanges(QueryStructReq queryStructReq, QueryStatement queryStatement) {
        queryStatement.setTimeRanges(queryStructUtils.getTimeRanges(queryStructReq));
    }

    protected String getVariablePrefix() {
        return queryStructUtils.getVariablePrefix();
    }
}
