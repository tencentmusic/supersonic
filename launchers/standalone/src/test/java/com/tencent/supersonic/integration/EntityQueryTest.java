package com.tencent.supersonic.integration;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.rule.entity.EntityFilterQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricEntityQuery;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.util.DataUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class EntityQueryTest extends BaseQueryTest {

    @Test
    public void queryTest_metric_entity_query() throws Exception {
        QueryResult actualResult = submitNewChat("艺人周杰伦的播放量");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricEntityQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        QueryFilter dimensionFilter = DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS, "周杰伦", "歌手名", 7L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        SchemaElement metric = SchemaElement.builder().name("播放量").build();
        expectedParseInfo.getMetrics().add(metric);

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateMode.BETWEEN, 1, period, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_entity_list_filter() throws Exception {
        QueryResult actualResult = submitNewChat("爱情、流行类型的艺人");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(EntityFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        List<String> list = new ArrayList<>();
        list.add("爱情");
        list.add("流行");
        QueryFilter dimensionFilter = DataUtils.getFilter("genre", FilterOperatorEnum.IN, list, "风格", 6L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        SchemaElement metric = SchemaElement.builder().name("播放量").build();
        expectedParseInfo.getMetrics().add(metric);

        SchemaElement dim1 = SchemaElement.builder().name("歌手名").build();
        SchemaElement dim2 = SchemaElement.builder().name("活跃区域").build();
        SchemaElement dim3 = SchemaElement.builder().name("风格").build();
        SchemaElement dim4 = SchemaElement.builder().name("代表作").build();
        expectedParseInfo.getDimensions().add(dim1);
        expectedParseInfo.getDimensions().add(dim2);
        expectedParseInfo.getDimensions().add(dim3);
        expectedParseInfo.getDimensions().add(dim4);

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, startDay, startDay));
        expectedParseInfo.setNativeQuery(true);

        assertQueryResult(expectedResult, actualResult);
    }

}
