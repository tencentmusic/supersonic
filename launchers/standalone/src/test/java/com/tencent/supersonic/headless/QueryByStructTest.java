package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.util.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class QueryByStructTest extends BaseTest {

    @Test
    @Order(0)
    public void testCacheQuery() {
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("部门"));
        QueryStructReq queryStructReq2 = buildQueryStructReq(Arrays.asList("部门"));
        QueryCache queryCache = ComponentFactory.getQueryCache();
        String cacheKey1 = queryCache.getCacheKey(queryStructReq1);
        String cacheKey2 = queryCache.getCacheKey(queryStructReq2);
        Assertions.assertEquals(cacheKey1, cacheKey2);
    }

    @Test
    public void testDetailQuery() throws Exception {
        QueryStructReq queryStructReq =
                buildQueryStructReq(Arrays.asList("用户名", "部门"), QueryType.DETAIL);
        SemanticQueryResp semanticQueryResp =
                semanticLayerService.queryByReq(queryStructReq, User.getDefaultUser());
        assertEquals(3, semanticQueryResp.getColumns().size());
        QueryColumn firstColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("用户名", firstColumn.getName());
        QueryColumn secondColumn = semanticQueryResp.getColumns().get(1);
        assertEquals("部门", secondColumn.getName());
        assertTrue(semanticQueryResp.getResultList().size() > 0);
    }

    @Test
    public void testSumQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(null);
        SemanticQueryResp semanticQueryResp =
                semanticLayerService.queryByReq(queryStructReq, User.getDefaultUser());
        assertEquals(1, semanticQueryResp.getColumns().size());
        QueryColumn queryColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("停留时长", queryColumn.getName());
        assertEquals(1, semanticQueryResp.getResultList().size());
    }

    @Test
    public void testGroupByQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("部门"));
        SemanticQueryResp result =
                semanticLayerService.queryByReq(queryStructReq, User.getDefaultUser());
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("停留时长", secondColumn.getName());
        assertNotNull(result.getResultList().size());
    }

    @Test
    public void testFilterQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("部门"));
        List<Filter> dimensionFilters = new ArrayList<>();
        Filter filter = new Filter();
        filter.setName("部门");
        filter.setBizName("department");
        filter.setOperator(FilterOperatorEnum.EQUALS);
        filter.setValue("HR");
        dimensionFilters.add(filter);
        queryStructReq.setDimensionFilters(dimensionFilters);

        SemanticQueryResp result =
                semanticLayerService.queryByReq(queryStructReq, User.getDefaultUser());
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("停留时长", secondColumn.getName());
        assertEquals(1, result.getResultList().size());
        assertEquals("HR", result.getResultList().get(0).get("部门").toString());
    }

    @Test
    public void testAuthorization_model() {
        User alice = DataUtils.getUserAlice();
        setDomainNotOpenToAll();
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("部门"));
        assertThrows(InvalidPermissionException.class,
                () -> semanticLayerService.queryByReq(queryStructReq1, alice));
    }

    @Test
    public void testAuthorization_sensitive_metric() {
        User tom = DataUtils.getUserTom();
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("人均访问次数");
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("部门"), aggregator);
        assertThrows(InvalidPermissionException.class,
                () -> semanticLayerService.queryByReq(queryStructReq, tom));
    }

    @Test
    public void testAuthorization_row_permission() throws Exception {
        User tom = DataUtils.getUserTom();
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("停留时长");
        QueryStructReq queryStructReq1 =
                buildQueryStructReq(Collections.singletonList("部门"), aggregator);
        SemanticQueryResp semanticQueryResp = semanticLayerService.queryByReq(queryStructReq1, tom);
        Assertions.assertNotNull(semanticQueryResp.getQueryAuthorization().getMessage());
        Assertions.assertTrue(semanticQueryResp.getSql().contains("用户名 = 'tom'"));
    }
}
