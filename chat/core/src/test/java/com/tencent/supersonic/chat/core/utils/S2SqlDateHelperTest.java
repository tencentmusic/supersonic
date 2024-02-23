package com.tencent.supersonic.chat.core.utils;


import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
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
        Assert.assertNotNull(referenceDate);

        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertNotNull(referenceDate);

        ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
        QueryConfig queryConfig = viewSchema.getQueryConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setPeriod(Constants.DAY);
        timeDefaultConfig.setUnit(20);
        queryConfig.getTagTypeDefaultConfig().setTimeDefaultConfig(timeDefaultConfig);

        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertNotNull(referenceDate);

        timeDefaultConfig.setUnit(1);
        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertNotNull(referenceDate);

        timeDefaultConfig.setUnit(-1);
        referenceDate = S2SqlDateHelper.getReferenceDate(queryContext, viewId);
        Assert.assertNull(referenceDate);
    }

    @Test
    void getStartEndDate() {
        Long viewId = 1L;
        QueryContext queryContext = buildQueryContext(viewId);

        Pair<String, String> startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, null, QueryType.TAG);
        Assert.assertNotNull(startEndDate);

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.TAG);
        Assert.assertNotNull(startEndDate);

        ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
        QueryConfig queryConfig = viewSchema.getQueryConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setPeriod(Constants.DAY);
        timeDefaultConfig.setUnit(20);
        queryConfig.getTagTypeDefaultConfig().setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.getMetricTypeDefaultConfig().setTimeDefaultConfig(timeDefaultConfig);

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.TAG);
        Assert.assertNotNull(startEndDate);

        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertNotNull(startEndDate);

        timeDefaultConfig.setUnit(1);
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertNotNull(startEndDate);

        timeDefaultConfig.setUnit(-1);
        startEndDate = S2SqlDateHelper.getStartEndDate(queryContext, viewId, QueryType.METRIC);
        Assert.assertNull(startEndDate.getLeft());
        Assert.assertNull(startEndDate.getRight());
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