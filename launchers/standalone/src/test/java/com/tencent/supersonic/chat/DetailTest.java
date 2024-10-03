package com.tencent.supersonic.chat;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.chat.query.rule.detail.DetailDimensionQuery;
import com.tencent.supersonic.headless.chat.query.rule.detail.DetailFilterQuery;
import com.tencent.supersonic.headless.chat.query.rule.detail.DetailIdQuery;
import com.tencent.supersonic.util.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class DetailTest extends BaseTest {

    @Test
    public void test_detail_dimension() throws Exception {
        QueryResult actualResult = submitNewChat("周杰伦流派和代表作", DataUtils.tagAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(DetailDimensionQuery.QUERY_MODE);
        expectedParseInfo.setQueryType(QueryType.DETAIL);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        QueryFilter dimensionFilter =
                DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS, "周杰伦", "歌手名", 8L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        expectedParseInfo.getDimensions()
                .addAll(Lists.newArrayList(SchemaElement.builder().name("流派").build(),
                        SchemaElement.builder().name("代表作").build()));

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void test_detail_id() throws Exception {
        QueryResult actualResult = submitNewChat("周杰伦", DataUtils.tagAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(DetailIdQuery.QUERY_MODE);
        expectedParseInfo.setQueryType(QueryType.DETAIL);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        QueryFilter dimensionFilter =
                DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS, "周杰伦", "歌手名", 8L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        expectedParseInfo.getMetrics().add(SchemaElement.builder().name("播放量").build());
        expectedParseInfo.getDimensions()
                .addAll(Lists.newArrayList(SchemaElement.builder().name("歌手名").build(),
                        SchemaElement.builder().name("活跃区域").build(),
                        SchemaElement.builder().name("流派").build(),
                        SchemaElement.builder().name("代表作").build()));

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void test_detail_list_filter() throws Exception {
        QueryResult actualResult = submitNewChat("国风歌手", DataUtils.tagAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(DetailFilterQuery.QUERY_MODE);
        expectedParseInfo.setQueryType(QueryType.DETAIL);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        QueryFilter dimensionFilter =
                DataUtils.getFilter("genre", FilterOperatorEnum.EQUALS, "国风", "流派", 7L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        expectedParseInfo.getMetrics().add(SchemaElement.builder().name("播放量").build());
        expectedParseInfo.getDimensions()
                .addAll(Lists.newArrayList(SchemaElement.builder().name("歌手名").build(),
                        SchemaElement.builder().name("活跃区域").build(),
                        SchemaElement.builder().name("流派").build(),
                        SchemaElement.builder().name("代表作").build()));

        assertQueryResult(expectedResult, actualResult);
    }
}
