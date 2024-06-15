package com.tencent.supersonic.chat;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.chat.query.rule.detail.DetailFilterQuery;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Test;

public class TagTest extends BaseTest {

    @Test
    public void queryTest_tag_list_filter() throws Exception {
        QueryResult actualResult = submitNewChat("爱情、流行类型的艺人", DataUtils.tagAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(DetailFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        QueryFilter dimensionFilter = DataUtils.getFilter("genre", FilterOperatorEnum.EQUALS,
                "流行", "风格", 6L);
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

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, startDay, startDay, 7));
        expectedParseInfo.setQueryType(QueryType.DETAIL);

        assertQueryResult(expectedResult, actualResult);
    }

}
