package com.tencent.supersonic.integration;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.rule.entity.EntityFilterQuery;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;

public class EntityQueryTest extends BaseQueryTest {

    @Test
    public void queryTest_ENTITY_LIST_FILTER()throws Exception {
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

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(1, DateConf.DateMode.RECENT_UNITS, "DAY"));
        expectedParseInfo.setNativeQuery(true);

        assertQueryResult(expectedResult, actualResult);
    }

}
