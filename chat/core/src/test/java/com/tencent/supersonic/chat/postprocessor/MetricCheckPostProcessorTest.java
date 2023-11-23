package com.tencent.supersonic.chat.postprocessor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.RelateSchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

class MetricCheckPostProcessorTest {

    @Test
    void testProcessCorrectSql_necessaryDimension_groupBy() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 用户名, sum(访问次数), count(distinct 访问用户数) from 超音数 group by 用户名";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql, mockModelSchema());
        String expectedProcessedSql = "SELECT 用户名, sum(访问次数) FROM 超音数 GROUP BY 用户名";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_necessaryDimension_where() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 用户名, sum(访问次数), count(distinct 访问用户数) from 超音数 where 部门 = 'HR' group by 用户名";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql, mockModelSchema());
        String expectedProcessedSql = "SELECT 用户名, sum(访问次数), count(DISTINCT 访问用户数) FROM 超音数 "
                + "WHERE 部门 = 'HR' GROUP BY 用户名";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_dimensionNotDrillDown_groupBy() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 页面, 部门, sum(访问次数), count(distinct 访问用户数) from 超音数 group by 页面, 部门";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql, mockModelSchema());
        String expectedProcessedSql = "SELECT 部门, sum(访问次数), count(DISTINCT 访问用户数) FROM 超音数 GROUP BY 部门";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_dimensionNotDrillDown_where() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 部门, sum(访问次数), count(distinct 访问用户数) from 超音数 where 页面 = 'P1' group by 部门";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql, mockModelSchema());
        String expectedProcessedSql = "SELECT 部门, sum(访问次数), count(DISTINCT 访问用户数) FROM 超音数 GROUP BY 部门";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_dimensionNotDrillDown_necessaryDimension() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 页面, sum(访问次数), count(distinct 访问用户数) from 超音数 group by 页面";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql, mockModelSchema());
        String expectedProcessedSql = "SELECT sum(访问次数) FROM 超音数";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_dimensionDrillDown() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 用户名, 部门, sum(访问次数), count(distinct 访问用户数) from 超音数 group by 用户名, 部门";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql, mockModelSchema());
        String expectedProcessedSql = "SELECT 用户名, 部门, sum(访问次数), count(DISTINCT 访问用户数) FROM 超音数 GROUP BY 用户名, 部门";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_noDrillDownDimensionSetting() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 页面, 用户名, sum(访问次数), count(distinct 访问用户数) from 超音数 group by 页面, 用户名";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql,
                mockModelSchemaNoDimensionSetting());
        String expectedProcessedSql = "SELECT 页面, 用户名, sum(访问次数), count(DISTINCT 访问用户数) FROM 超音数 GROUP BY 页面, 用户名";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_noDrillDownDimensionSetting_noAgg() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 访问次数 from 超音数";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql,
                mockModelSchemaNoDimensionSetting());
        String expectedProcessedSql = "select 访问次数 from 超音数";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    @Test
    void testProcessCorrectSql_noDrillDownDimensionSetting_count() {
        MetricCheckPostProcessor metricCheckPostProcessor = new MetricCheckPostProcessor();
        String correctSql = "select 部门, count(*) from 超音数 group by 部门";
        String actualProcessedSql = metricCheckPostProcessor.processCorrectSql(correctSql,
                mockModelSchemaNoDimensionSetting());
        String expectedProcessedSql = "select 部门, count(*) from 超音数 group by 部门";
        Assertions.assertEquals(expectedProcessedSql, actualProcessedSql);
    }

    /**
     * 访问次数 drill down dimension is 用户名 and 部门
     * 访问用户数 drill down dimension is 部门, and 部门 is necessary, 部门 need in select and group by or where expressions
     */
    private ModelSchema mockModelSchema() {
        ModelSchema modelSchema = new ModelSchema();
        Set<SchemaElement> metrics = Sets.newHashSet(
                mockElement(1L, "访问次数", SchemaElementType.METRIC,
                        Lists.newArrayList(RelateSchemaElement.builder().dimensionId(2L).isNecessary(false).build(),
                                RelateSchemaElement.builder().dimensionId(1L).isNecessary(false).build())),
                mockElement(2L, "访问用户数", SchemaElementType.METRIC,
                        Lists.newArrayList(RelateSchemaElement.builder().dimensionId(2L).isNecessary(true).build()))
        );
        modelSchema.setMetrics(metrics);
        modelSchema.setDimensions(mockDimensions());
        return modelSchema;
    }

    private ModelSchema mockModelSchemaNoDimensionSetting() {
        ModelSchema modelSchema = new ModelSchema();
        Set<SchemaElement> metrics = Sets.newHashSet(
                mockElement(1L, "访问次数", SchemaElementType.METRIC, Lists.newArrayList()),
                mockElement(2L, "访问用户数", SchemaElementType.METRIC, Lists.newArrayList())
        );
        modelSchema.setMetrics(metrics);
        modelSchema.setDimensions(mockDimensions());
        return modelSchema;
    }

    private Set<SchemaElement> mockDimensions() {
        return Sets.newHashSet(
                mockElement(1L, "用户名", SchemaElementType.DIMENSION, Lists.newArrayList()),
                mockElement(2L, "部门", SchemaElementType.DIMENSION, Lists.newArrayList()),
                mockElement(3L, "页面", SchemaElementType.DIMENSION, Lists.newArrayList())
        );
    }

    private SchemaElement mockElement(Long id, String name, SchemaElementType type,
                                      List<RelateSchemaElement> relateSchemaElements) {
        return SchemaElement.builder().id(id).name(name).type(type)
                .relateSchemaElements(relateSchemaElements).build();
    }

}