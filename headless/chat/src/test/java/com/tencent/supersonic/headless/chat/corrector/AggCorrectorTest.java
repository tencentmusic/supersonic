package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AggCorrectorTest {

    @Test
    void testDoCorrect() {
        AggCorrector corrector = new AggCorrector();
        Long dataSetId = 1L;
        ChatQueryContext chatQueryContext = buildQueryContext(dataSetId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSetId(dataSetId);
        semanticParseInfo.setDataSet(dataSet);
        SqlInfo sqlInfo = new SqlInfo();
        String sql = "SELECT 用户, 访问次数 FROM 超音数数据集 WHERE 部门 = 'sales' AND"
                + " datediff('day', 数据日期, '2024-06-04') <= 7"
                + " GROUP BY 用户 ORDER BY SUM(访问次数) DESC LIMIT 1";
        sqlInfo.setParsedS2SQL(sql);
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        corrector.correct(chatQueryContext, semanticParseInfo);
        Assert.assertEquals(
                "SELECT 用户, SUM(访问次数) FROM 超音数数据集 WHERE 部门 = 'sales'"
                        + " AND datediff('day', 数据日期, '2024-06-04') <= 7 GROUP BY 用户"
                        + " ORDER BY SUM(访问次数) DESC LIMIT 1",
                semanticParseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    private ChatQueryContext buildQueryContext(Long dataSetId) {
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        dataSetSchema.setDataSet(schemaElement);
        Set<SchemaElement> dimensions = new HashSet<>();
        SchemaElement element1 = new SchemaElement();
        element1.setDataSetId(1L);
        element1.setName("部门");
        dimensions.add(element1);

        dataSetSchema.setDimensions(dimensions);

        Set<SchemaElement> metrics = new HashSet<>();
        SchemaElement metric1 = new SchemaElement();
        metric1.setDataSetId(1L);
        metric1.setName("访问次数");
        metrics.add(metric1);

        dataSetSchema.setMetrics(metrics);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        chatQueryContext.setSemanticSchema(semanticSchema);
        return chatQueryContext;
    }
}
