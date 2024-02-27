package com.tencent.supersonic.chat.core.utils;


import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class S2SqlDateHelperTest {

    @Test
    void getReferenceDate() {
        Long viewId = 1L;
        QueryContext queryContext = buildQueryContext(viewId);

        String referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, null);
        Assert.assertEquals(referenceDate, DateUtils.getBeforeDate(0));

        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertEquals(referenceDate, DateUtils.getBeforeDate(0));

        ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
        QueryConfig queryConfig = viewSchema.getQueryConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setPeriod(Constants.DAY);
        timeDefaultConfig.setUnit(20);
        queryConfig.getTagTypeDefaultConfig().setTimeDefaultConfig(timeDefaultConfig);

        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertEquals(referenceDate, DateUtils.getBeforeDate(20));

        timeDefaultConfig.setUnit(1);
        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertEquals(referenceDate, DateUtils.getBeforeDate(1));

        timeDefaultConfig.setUnit(-1);
        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertNull(referenceDate);
    }

    @Test
    void getStartEndDate() {
        Long viewId = 1L;
        QueryContext queryContext = buildQueryContext(viewId);

        Pair<String, String> startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, null, QueryType.TAG);
        Assert.assertEquals(startEndDate.getLeft(), DateUtils.getBeforeDate(0));
        Assert.assertEquals(startEndDate.getRight(), DateUtils.getBeforeDate(0));

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.TAG);
        Assert.assertNull(startEndDate.getLeft());
        Assert.assertNull(startEndDate.getRight());

        ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
        QueryConfig queryConfig = viewSchema.getQueryConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setPeriod(Constants.DAY);
        timeDefaultConfig.setUnit(20);
        queryConfig.getTagTypeDefaultConfig().setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.getMetricTypeDefaultConfig().setTimeDefaultConfig(timeDefaultConfig);

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.TAG);
        Assert.assertEquals(startEndDate.getLeft(), DateUtils.getBeforeDate(20));
        Assert.assertEquals(startEndDate.getRight(), DateUtils.getBeforeDate(20));

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertEquals(startEndDate.getLeft(), DateUtils.getBeforeDate(20));
        Assert.assertEquals(startEndDate.getRight(), DateUtils.getBeforeDate(20));

        timeDefaultConfig.setUnit(2);
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertEquals(startEndDate.getLeft(), DateUtils.getBeforeDate(2));
        Assert.assertEquals(startEndDate.getRight(), DateUtils.getBeforeDate(1));

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.TAG);
        Assert.assertEquals(startEndDate.getLeft(), DateUtils.getBeforeDate(2));
        Assert.assertEquals(startEndDate.getRight(), DateUtils.getBeforeDate(1));

        timeDefaultConfig.setUnit(-1);
        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertNull(startEndDate.getLeft());
        Assert.assertNull(startEndDate.getRight());

        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setPeriod(Constants.DAY);
        timeDefaultConfig.setUnit(5);
        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertEquals(startEndDate.getLeft(), DateUtils.getBeforeDate(5));
        Assert.assertEquals(startEndDate.getRight(), DateUtils.getBeforeDate(5));
    }

    private QueryContext buildQueryContext(Long viewId) {
        QueryContext queryContext = new QueryContext();
        List<ViewSchema> viewSchemaList = new ArrayList<>();
        ViewSchema viewSchema = new ViewSchema();
        QueryConfig queryConfig = new QueryConfig();
        viewSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setView(viewId);
        viewSchema.setView(schemaElement);
        viewSchemaList.add(viewSchema);

        SemanticSchema semanticSchema = new SemanticSchema(viewSchemaList);
        queryContext.setSemanticSchema(semanticSchema);
        return queryContext;
    }
}