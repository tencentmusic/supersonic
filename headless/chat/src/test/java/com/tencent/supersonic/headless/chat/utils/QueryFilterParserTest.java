package com.tencent.supersonic.headless.chat.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class QueryFilterParserTest {

    @Test
    void parse() {
        // Example usage
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

        String parse = QueryFilterParser.parse(queryFilters);

        Assert.assertEquals(parse, "age > 30 AND name LIKE 'John%' AND id IN (1, 2, 3, 4)"
                + " AND status NOT_IN ('inactive', 'deleted')");
    }
}
