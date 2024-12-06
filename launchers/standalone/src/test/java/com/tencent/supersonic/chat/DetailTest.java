package com.tencent.supersonic.chat;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.demo.S2SingerDemo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.chat.query.rule.detail.DetailDimensionQuery;
import com.tencent.supersonic.util.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class DetailTest extends BaseTest {

    @BeforeEach
    public void init() {
        agent = getAgentByName(S2SingerDemo.AGENT_NAME);
        schema = schemaService.getSemanticSchema(agent.getDataSetIds());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void test_detail_dimension() throws Exception {
        QueryResult actualResult = submitNewChat("周杰伦流派和代表作", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(DetailDimensionQuery.QUERY_MODE);
        expectedParseInfo.setQueryType(QueryType.DETAIL);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        SchemaElement singerElement = getSchemaElementByName(schema.getDimensions(), "歌手名");

        QueryFilter dimensionFilter = DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS,
                "周杰伦", "歌手名", singerElement.getId());
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        expectedParseInfo.getDimensions()
                .addAll(Lists.newArrayList(SchemaElement.builder().name("流派").build(),
                        SchemaElement.builder().name("代表作").build()));

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void test_detail_filter() throws Exception {
        QueryResult actualResult = submitNewChat("国风歌手", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(DetailDimensionQuery.QUERY_MODE);
        expectedParseInfo.setQueryType(QueryType.DETAIL);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        SchemaElement genreElement = getSchemaElementByName(schema.getDimensions(), "流派");
        QueryFilter dimensionFilter = DataUtils.getFilter("genre", FilterOperatorEnum.EQUALS, "国风",
                "流派", genreElement.getId());
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);
        expectedParseInfo.getDimensions()
                .addAll(Lists.newArrayList(SchemaElement.builder().name("歌手名").build()));

        assertQueryResult(expectedResult, actualResult);
    }
}
