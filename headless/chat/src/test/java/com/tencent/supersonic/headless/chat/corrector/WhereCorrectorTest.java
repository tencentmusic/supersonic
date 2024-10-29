package com.tencent.supersonic.headless.chat.corrector;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class WhereCorrectorTest {

    @Test
    void addQueryFilter() {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        String sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 <= '2023-11-17' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);

        ChatQueryContext chatQueryContext = new ChatQueryContext();

        QueryFilter filter1 = new QueryFilter();
        filter1.setName("age");
        filter1.setOperator(FilterOperatorEnum.GREATER_THAN);
        filter1.setValue(30);

        QueryFilter filter2 = new QueryFilter();
        filter2.setName("name");
        filter2.setOperator(FilterOperatorEnum.LIKE);
        filter2.setValue("John%");

        QueryFilter filter3 = new QueryFilter();
        filter3.setName("id");
        filter3.setOperator(FilterOperatorEnum.IN);
        filter3.setValue(Lists.newArrayList(1, 2, 3, 4));

        QueryFilter filter4 = new QueryFilter();
        filter4.setName("status");
        filter4.setOperator(FilterOperatorEnum.NOT_IN);
        filter4.setValue(Lists.newArrayList("inactive", "deleted"));

        QueryFilters queryFilters = new QueryFilters();
        queryFilters.getFilters().add(filter1);
        queryFilters.getFilters().add(filter2);
        queryFilters.getFilters().add(filter3);
        queryFilters.getFilters().add(filter4);
        chatQueryContext.getRequest().setQueryFilters(queryFilters);

        WhereCorrector whereCorrector = new WhereCorrector();
        whereCorrector.addQueryFilter(chatQueryContext, semanticParseInfo);

        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();

        Assert.assertEquals(correctS2SQL,
                "SELECT 维度1, SUM(播放量) FROM 数据库 WHERE "
                        + "(歌手名 = '张三') AND 数据日期 <= '2023-11-17' AND age > 30 AND "
                        + "name LIKE 'John%' AND id IN (1, 2, 3, 4) AND status GROUP BY 维度1");
    }
}
