package com.tencent.supersonic.headless.server.calcite;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.response.SqlParserResp;
import com.tencent.supersonic.headless.core.parser.calcite.planner.AggPlanner;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionTimeTypeParamsTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.IdentifyYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class HeadlessParserServiceTest {

    private static Map<String, SemanticSchema> headlessSchemaMap = new HashMap<>();

    public static SqlParserResp parser(SemanticSchema semanticSchema, MetricQueryParam metricQueryParam,
            boolean isAgg) {
        SqlParserResp sqlParser = new SqlParserResp();
        try {
            if (semanticSchema == null) {
                sqlParser.setErrMsg("headlessSchema not found");
                return sqlParser;
            }
            AggPlanner aggBuilder = new AggPlanner(semanticSchema);
            QueryStatement queryStatement = new QueryStatement();
            queryStatement.setMetricQueryParam(metricQueryParam);
            aggBuilder.explain(queryStatement, AggOption.getAggregation(!isAgg));
            EngineType engineType = EngineType.fromString(semanticSchema.getSemanticModel().getDatabase().getType());
            sqlParser.setSql(aggBuilder.getSql(engineType));
            sqlParser.setSourceId(aggBuilder.getSourceId());
        } catch (Exception e) {
            sqlParser.setErrMsg(e.getMessage());
            log.error("parser error metricQueryReq[{}] error [{}]", metricQueryParam, e);
        }
        return sqlParser;
    }

    public void test() throws Exception {

        DataModelYamlTpl datasource = new DataModelYamlTpl();
        datasource.setName("s2_pv_uv_statis");
        datasource.setSourceId(1L);
        datasource.setSqlQuery("SELECT imp_date, user_name,page,1 as pv, user_name as uv FROM s2_pv_uv_statis");

        MeasureYamlTpl measure = new MeasureYamlTpl();
        measure.setAgg("sum");
        measure.setName("s2_pv_uv_statis_pv");
        measure.setExpr("pv");
        List<MeasureYamlTpl> measures = new ArrayList<>();
        measures.add(measure);

        MeasureYamlTpl measure2 = new MeasureYamlTpl();
        measure2.setAgg("count");
        measure2.setName("s2_pv_uv_statis_internal_cnt");
        measure2.setExpr("1");
        measure2.setCreateMetric("true");
        measures.add(measure2);

        MeasureYamlTpl measure3 = new MeasureYamlTpl();
        measure3.setAgg("count");
        measure3.setName("s2_pv_uv_statis_uv");
        measure3.setExpr("uv");
        measure3.setCreateMetric("true");
        measures.add(measure3);

        datasource.setMeasures(measures);

        DimensionYamlTpl dimension = new DimensionYamlTpl();
        dimension.setName("imp_date");
        dimension.setExpr("imp_date");
        dimension.setType("time");
        DimensionTimeTypeParamsTpl dimensionTimeTypeParams = new DimensionTimeTypeParamsTpl();
        dimensionTimeTypeParams.setIsPrimary("true");
        dimensionTimeTypeParams.setTimeGranularity("day");
        dimension.setTypeParams(dimensionTimeTypeParams);
        List<DimensionYamlTpl> dimensions = new ArrayList<>();
        dimensions.add(dimension);

        DimensionYamlTpl dimension2 = new DimensionYamlTpl();
        dimension2.setName("sys_imp_date");
        dimension2.setExpr("imp_date");
        dimension2.setType("time");
        DimensionTimeTypeParamsTpl dimensionTimeTypeParams2 = new DimensionTimeTypeParamsTpl();
        dimensionTimeTypeParams2.setIsPrimary("true");
        dimensionTimeTypeParams2.setTimeGranularity("day");
        dimension2.setTypeParams(dimensionTimeTypeParams2);
        dimensions.add(dimension2);

        DimensionYamlTpl dimension3 = new DimensionYamlTpl();
        dimension3.setName("sys_imp_week");
        dimension3.setExpr("to_monday(from_unixtime(unix_timestamp(imp_date), 'yyyy-MM-dd'))");
        dimension3.setType("time");
        DimensionTimeTypeParamsTpl dimensionTimeTypeParams3 = new DimensionTimeTypeParamsTpl();
        dimensionTimeTypeParams3.setIsPrimary("true");
        dimensionTimeTypeParams3.setTimeGranularity("day");
        dimension3.setTypeParams(dimensionTimeTypeParams3);
        dimensions.add(dimension3);

        datasource.setDimensions(dimensions);

        List<IdentifyYamlTpl> identifies = new ArrayList<>();
        IdentifyYamlTpl identify = new IdentifyYamlTpl();
        identify.setName("user_name");
        identify.setType("primary");
        identifies.add(identify);
        datasource.setIdentifiers(identifies);
        SemanticSchema semanticSchema = SemanticSchema.newBuilder("1").build();

        SemanticSchemaManager.update(semanticSchema, SemanticSchemaManager.getDatasource(datasource));

        DimensionYamlTpl dimension1 = new DimensionYamlTpl();
        dimension1.setExpr("page");
        dimension1.setName("page");
        dimension1.setType("categorical");
        List<DimensionYamlTpl> dimensionYamlTpls = new ArrayList<>();
        dimensionYamlTpls.add(dimension1);

        SemanticSchemaManager.update(semanticSchema, "s2_pv_uv_statis",
                SemanticSchemaManager.getDimensions(dimensionYamlTpls));

        MetricYamlTpl metric1 = new MetricYamlTpl();
        metric1.setName("pv");
        metric1.setType("expr");
        MetricTypeParamsYamlTpl metricTypeParams = new MetricTypeParamsYamlTpl();
        List<MeasureYamlTpl> measures1 = new ArrayList<>();
        MeasureYamlTpl measure1 = new MeasureYamlTpl();
        measure1.setName("s2_pv_uv_statis_pv");
        measures1.add(measure1);
        metricTypeParams.setMeasures(measures1);
        metricTypeParams.setExpr("s2_pv_uv_statis_pv");
        metric1.setTypeParams(metricTypeParams);
        List<MetricYamlTpl> metric = new ArrayList<>();
        metric.add(metric1);

        MetricYamlTpl metric2 = new MetricYamlTpl();
        metric2.setName("uv");
        metric2.setType("expr");
        MetricTypeParamsYamlTpl metricTypeParams1 = new MetricTypeParamsYamlTpl();
        List<MeasureYamlTpl> measures2 = new ArrayList<>();
        MeasureYamlTpl measure4 = new MeasureYamlTpl();
        measure4.setName("s2_pv_uv_statis_uv");
        measures2.add(measure4);
        metricTypeParams1.setMeasures(measures2);
        metricTypeParams1.setExpr("s2_pv_uv_statis_uv");
        metric2.setTypeParams(metricTypeParams1);
        metric.add(metric2);

        //HeadlessSchemaManager.update(headlessSchema, HeadlessSchemaManager.getMetrics(metric));

        MetricQueryParam metricCommand = new MetricQueryParam();
        metricCommand.setDimensions(new ArrayList<>(Arrays.asList("sys_imp_date")));
        metricCommand.setMetrics(new ArrayList<>(Arrays.asList("pv")));
        metricCommand.setWhere("user_name = 'ab' and (sys_imp_date >= '2023-02-28' and sys_imp_date <= '2023-05-28') ");
        metricCommand.setLimit(1000L);
        List<ColumnOrder> orders = new ArrayList<>();
        orders.add(ColumnOrder.buildDesc("sys_imp_date"));
        metricCommand.setOrder(orders);
        System.out.println(parser(semanticSchema, metricCommand, true));

        addDepartment(semanticSchema);

        MetricQueryParam metricCommand2 = new MetricQueryParam();
        metricCommand2.setDimensions(new ArrayList<>(
                Arrays.asList("sys_imp_date", "user_name__department", "user_name", "user_name__page")));
        metricCommand2.setMetrics(new ArrayList<>(Arrays.asList("pv")));
        metricCommand2.setWhere(
                "user_name = 'ab' and (sys_imp_date >= '2023-02-28' and sys_imp_date <= '2023-05-28') ");
        metricCommand2.setLimit(1000L);
        List<ColumnOrder> orders2 = new ArrayList<>();
        orders2.add(ColumnOrder.buildDesc("sys_imp_date"));
        metricCommand2.setOrder(orders2);
        System.out.println(parser(semanticSchema, metricCommand2, true));


    }

    private static void addDepartment(SemanticSchema semanticSchema) {
        DataModelYamlTpl datasource = new DataModelYamlTpl();
        datasource.setName("user_department");
        datasource.setSourceId(1L);
        datasource.setSqlQuery("SELECT imp_date,user_name,department FROM s2_user_department");

        MeasureYamlTpl measure = new MeasureYamlTpl();
        measure.setAgg("count");
        measure.setName("user_department_internal_cnt");
        measure.setCreateMetric("true");
        measure.setExpr("1");
        List<MeasureYamlTpl> measures = new ArrayList<>();
        measures.add(measure);

        datasource.setMeasures(measures);

        DimensionYamlTpl dimension = new DimensionYamlTpl();
        dimension.setName("sys_imp_date");
        dimension.setExpr("imp_date");
        dimension.setType("time");
        DimensionTimeTypeParamsTpl dimensionTimeTypeParams = new DimensionTimeTypeParamsTpl();
        dimensionTimeTypeParams.setIsPrimary("true");
        dimensionTimeTypeParams.setTimeGranularity("day");
        dimension.setTypeParams(dimensionTimeTypeParams);
        List<DimensionYamlTpl> dimensions = new ArrayList<>();
        dimensions.add(dimension);

        DimensionYamlTpl dimension3 = new DimensionYamlTpl();
        dimension3.setName("sys_imp_week");
        dimension3.setExpr("to_monday(from_unixtime(unix_timestamp(imp_date), 'yyyy-MM-dd'))");
        dimension3.setType("time");
        DimensionTimeTypeParamsTpl dimensionTimeTypeParams3 = new DimensionTimeTypeParamsTpl();
        dimensionTimeTypeParams3.setIsPrimary("true");
        dimensionTimeTypeParams3.setTimeGranularity("week");
        dimension3.setTypeParams(dimensionTimeTypeParams3);
        dimensions.add(dimension3);

        datasource.setDimensions(dimensions);

        List<IdentifyYamlTpl> identifies = new ArrayList<>();
        IdentifyYamlTpl identify = new IdentifyYamlTpl();
        identify.setName("user_name");
        identify.setType("primary");
        identifies.add(identify);
        datasource.setIdentifiers(identifies);

        semanticSchema.getDatasource().put("user_department", SemanticSchemaManager.getDatasource(datasource));

        DimensionYamlTpl dimension1 = new DimensionYamlTpl();
        dimension1.setExpr("department");
        dimension1.setName("department");
        dimension1.setType("categorical");
        List<DimensionYamlTpl> dimensionYamlTpls = new ArrayList<>();
        dimensionYamlTpls.add(dimension1);

        semanticSchema.getDimension()
                .put("user_department", SemanticSchemaManager.getDimensions(dimensionYamlTpls));
    }
}
