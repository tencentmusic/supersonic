package com.tencent.supersonic.headless.server.aspect;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.utils.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Slf4j
public class MetricDrillDownCheckerTest {

    @Test
    void test_groupBy_in_drillDownDimension() {
        MetricDrillDownChecker metricDrillDownChecker = new MetricDrillDownChecker();
        String sql = "select user_name, sum(pv) from t_1 group by user_name";
        SemanticSchemaResp semanticSchemaResp = mockModelSchemaResp();
        metricDrillDownChecker.checkQuery(semanticSchemaResp, sql);
    }

    @Test
    void test_groupBy_not_in_drillDownDimension() {
        MetricDrillDownChecker metricDrillDownChecker = new MetricDrillDownChecker();
        String sql = "select page, sum(pv) from t_1 group by page";
        SemanticSchemaResp semanticSchemaResp = mockModelSchemaResp();
        assertThrows(InvalidArgumentException.class,
                () -> metricDrillDownChecker.checkQuery(semanticSchemaResp, sql));
    }

    @Test
    void test_groupBy_not_in_necessary_dimension() {
        MetricDrillDownChecker metricDrillDownChecker = new MetricDrillDownChecker();
        String sql = "select user_name, count(distinct uv) from t_1 group by user_name";
        SemanticSchemaResp semanticSchemaResp = mockModelSchemaResp();
        assertThrows(InvalidArgumentException.class,
                () -> metricDrillDownChecker.checkQuery(semanticSchemaResp, sql));
    }

    @Test
    void test_groupBy_no_necessary_dimension_setting() {
        MetricDrillDownChecker metricDrillDownChecker = new MetricDrillDownChecker();
        String sql = "select user_name, page, count(distinct uv) from t_1 group by user_name,page";
        SemanticSchemaResp semanticSchemaResp = mockModelSchemaNoDimensionSetting();
        metricDrillDownChecker.checkQuery(semanticSchemaResp, sql);
    }

    @Test
    void test_groupBy_no_necessary_dimension_setting_no_metric() {
        MetricDrillDownChecker metricDrillDownChecker = new MetricDrillDownChecker();
        String sql = "select user_name, page, count(*) from t_1 group by user_name,page";
        SemanticSchemaResp semanticSchemaResp = mockModelSchemaNoDimensionSetting();
        metricDrillDownChecker.checkQuery(semanticSchemaResp, sql);
    }

    private SemanticSchemaResp mockModelSchemaResp() {
        SemanticSchemaResp semanticSchemaResp = new SemanticSchemaResp();
        semanticSchemaResp.setMetrics(mockMetrics());
        semanticSchemaResp.setDimensions(mockDimensions());
        return semanticSchemaResp;
    }

    private SemanticSchemaResp mockModelSchemaNoDimensionSetting() {
        SemanticSchemaResp semanticSchemaResp = new SemanticSchemaResp();
        List<MetricSchemaResp> metricSchemaResps = Lists.newArrayList(mockMetricsNoDrillDownSetting());
        semanticSchemaResp.setMetrics(metricSchemaResps);
        semanticSchemaResp.setDimensions(mockDimensions());
        return semanticSchemaResp;
    }

    private List<DimSchemaResp> mockDimensions() {
        return Lists.newArrayList(DataUtils.mockDimension(1L, "user_name", "用户名"),
                DataUtils.mockDimension(2L, "department", "部门"),
                DataUtils.mockDimension(3L, "page", "页面"));
    }

    private List<MetricSchemaResp> mockMetrics() {
        return Lists.newArrayList(
                DataUtils.mockMetric(1L, "pv", "访问次数",
                        Lists.newArrayList(new DrillDownDimension(1L), new DrillDownDimension(2L))),
                DataUtils.mockMetric(2L, "uv", "访问用户数",
                        Lists.newArrayList(new DrillDownDimension(2L, true))));
    }

    private List<MetricSchemaResp> mockMetricsNoDrillDownSetting() {
        return Lists.newArrayList(
                DataUtils.mockMetric(1L, "pv", Lists.newArrayList()),
                DataUtils.mockMetric(2L, "uv", Lists.newArrayList()));
    }

}
