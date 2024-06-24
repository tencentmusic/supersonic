package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.DefaultDisplayInfo;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.chat.QueryContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class SelectCorrectorTest {

    Long dataSetId = 2L;

    @Test
    void testDoCorrect() {
        BaseSemanticCorrector corrector = new SelectCorrector();
        QueryContext queryContext = buildQueryContext(dataSetId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSet(dataSetId);
        semanticParseInfo.setDataSet(dataSet);
        semanticParseInfo.setQueryType(QueryType.DETAIL);
        SqlInfo sqlInfo = new SqlInfo();
        String sql = "SELECT * FROM 艺人库 WHERE 艺人名='周杰伦'";
        sqlInfo.setS2SQL(sql);
        sqlInfo.setCorrectS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        corrector.correct(queryContext, semanticParseInfo);
        Assert.assertEquals("SELECT 粉丝数, 国籍, 艺人名, 性别 FROM 艺人库 WHERE 艺人名 = '周杰伦'",
                semanticParseInfo.getSqlInfo().getCorrectS2SQL());
    }

    private QueryContext buildQueryContext(Long dataSetId) {
        QueryContext queryContext = new QueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        TagTypeDefaultConfig tagTypeDefaultConfig = new TagTypeDefaultConfig();
        DefaultDisplayInfo defaultDisplayInfo = new DefaultDisplayInfo();
        List<Long> dimensionIds = new ArrayList<>();
        dimensionIds.add(1L);
        dimensionIds.add(2L);
        dimensionIds.add(3L);
        defaultDisplayInfo.setDimensionIds(dimensionIds);

        List<Long> metricIds = new ArrayList<>();
        metricIds.add(4L);
        defaultDisplayInfo.setMetricIds(metricIds);

        tagTypeDefaultConfig.setDefaultDisplayInfo(defaultDisplayInfo);
        queryConfig.setTagTypeDefaultConfig(tagTypeDefaultConfig);

        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSet(dataSetId);
        dataSetSchema.setDataSet(schemaElement);
        Set<SchemaElement> dimensions = new HashSet<>();
        SchemaElement element1 = new SchemaElement();
        element1.setDataSet(dataSetId);
        element1.setId(1L);
        element1.setName("艺人名");
        dimensions.add(element1);

        SchemaElement element2 = new SchemaElement();
        element2.setDataSet(dataSetId);
        element2.setId(2L);
        element2.setName("性别");
        dimensions.add(element2);

        SchemaElement element3 = new SchemaElement();
        element3.setDataSet(dataSetId);
        element3.setId(3L);
        element3.setName("国籍");
        dimensions.add(element3);

        dataSetSchema.setDimensions(dimensions);

        Set<SchemaElement> metrics = new HashSet<>();
        SchemaElement metric1 = new SchemaElement();
        metric1.setDataSet(dataSetId);
        metric1.setId(4L);
        metric1.setName("粉丝数");
        metrics.add(metric1);

        dataSetSchema.setMetrics(metrics);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        queryContext.setSemanticSchema(semanticSchema);
        return queryContext;
    }
}