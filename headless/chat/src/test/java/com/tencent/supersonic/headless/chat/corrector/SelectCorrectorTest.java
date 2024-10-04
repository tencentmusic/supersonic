package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.DetailTypeDefaultConfig;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

class SelectCorrectorTest {

    Long dataSetId = 2L;

    @Test
    void testDoCorrect() {
        MockedStatic<ContextUtils> mocked = Mockito.mockStatic(ContextUtils.class);
        Environment mockEnvironment = Mockito.mock(Environment.class);
        mocked.when(() -> ContextUtils.getBean(Environment.class)).thenReturn(mockEnvironment);
        when(mockEnvironment.getProperty(SelectCorrector.ADDITIONAL_INFORMATION)).thenReturn("");

        BaseSemanticCorrector corrector = new SelectCorrector();
        ChatQueryContext chatQueryContext = buildQueryContext(dataSetId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSetId(dataSetId);
        semanticParseInfo.setDataSet(dataSet);
        semanticParseInfo.setQueryType(QueryType.DETAIL);
        SqlInfo sqlInfo = new SqlInfo();
        String sql = "SELECT * FROM 艺人库 WHERE 艺人名='周杰伦'";
        sqlInfo.setParsedS2SQL(sql);
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        corrector.correct(chatQueryContext, semanticParseInfo);
        Assert.assertEquals("SELECT 粉丝数, 国籍, 艺人名, 性别 FROM 艺人库 WHERE 艺人名 = '周杰伦'",
                semanticParseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    private ChatQueryContext buildQueryContext(Long dataSetId) {
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        DetailTypeDefaultConfig detailTypeDefaultConfig = new DetailTypeDefaultConfig();
        DefaultDisplayInfo defaultDisplayInfo = new DefaultDisplayInfo();
        List<Long> dimensionIds = new ArrayList<>();
        dimensionIds.add(1L);
        dimensionIds.add(2L);
        dimensionIds.add(3L);
        defaultDisplayInfo.setDimensionIds(dimensionIds);

        List<Long> metricIds = new ArrayList<>();
        metricIds.add(4L);
        defaultDisplayInfo.setMetricIds(metricIds);

        detailTypeDefaultConfig.setDefaultDisplayInfo(defaultDisplayInfo);
        queryConfig.setDetailTypeDefaultConfig(detailTypeDefaultConfig);

        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        dataSetSchema.setDataSet(schemaElement);
        Set<SchemaElement> dimensions = new HashSet<>();
        SchemaElement element1 = new SchemaElement();
        element1.setDataSetId(dataSetId);
        element1.setId(1L);
        element1.setName("艺人名");
        dimensions.add(element1);

        SchemaElement element2 = new SchemaElement();
        element2.setDataSetId(dataSetId);
        element2.setId(2L);
        element2.setName("性别");
        dimensions.add(element2);

        SchemaElement element3 = new SchemaElement();
        element3.setDataSetId(dataSetId);
        element3.setId(3L);
        element3.setName("国籍");
        dimensions.add(element3);

        dataSetSchema.setDimensions(dimensions);

        Set<SchemaElement> metrics = new HashSet<>();
        SchemaElement metric1 = new SchemaElement();
        metric1.setDataSetId(dataSetId);
        metric1.setId(4L);
        metric1.setName("粉丝数");
        metrics.add(metric1);

        dataSetSchema.setMetrics(metrics);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        chatQueryContext.setSemanticSchema(semanticSchema);
        return chatQueryContext;
    }
}
