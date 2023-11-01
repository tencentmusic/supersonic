package com.tencent.supersonic.integration.mapper;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.rule.metric.MetricEntityQuery;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.integration.BaseQueryTest;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Test;

public class MapperTest extends BaseQueryTest {

    @Test
    public void hanlp() throws Exception {

        QueryReq queryContextReq = DataUtils.getQueryContextReq(10, "艺人周杰伦的播放量");
        queryContextReq.setAgentId(1);

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

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, 7, period, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

}
