package com.tencent.supersonic.headless;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class QueryByStructTest extends BaseTest {

    @Test
    public void testDetailQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("user_name", "department"),
                QueryType.DETAIL);
        SemanticQueryResp semanticQueryResp = queryService.queryByReq(queryStructReq, User.getFakeUser());
        assertEquals(3, semanticQueryResp.getColumns().size());
        QueryColumn firstColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("用户", firstColumn.getName());
        QueryColumn secondColumn = semanticQueryResp.getColumns().get(1);
        assertEquals("部门", secondColumn.getName());
        QueryColumn thirdColumn = semanticQueryResp.getColumns().get(2);
        assertEquals("访问次数", thirdColumn.getName());
        assertTrue(semanticQueryResp.getResultList().size() > 0);
    }

    @Test
    public void testSumQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(null);
        SemanticQueryResp semanticQueryResp = queryService.queryByReq(queryStructReq, User.getFakeUser());
        assertEquals(1, semanticQueryResp.getColumns().size());
        QueryColumn queryColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("访问次数", queryColumn.getName());
        assertEquals(1, semanticQueryResp.getResultList().size());
    }

    @Test
    public void testGroupByQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("department"));
        SemanticQueryResp result = queryService.queryByReq(queryStructReq, User.getFakeUser());
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("访问次数", secondColumn.getName());
        assertNotNull(result.getResultList().size());
    }

    @Test
    public void testFilterQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("department"));
        List<Filter> dimensionFilters = new ArrayList<>();
        Filter filter = new Filter();
        filter.setName("部门");
        filter.setBizName("department");
        filter.setOperator(FilterOperatorEnum.EQUALS);
        filter.setValue("HR");
        dimensionFilters.add(filter);
        queryStructReq.setDimensionFilters(dimensionFilters);

        SemanticQueryResp result = queryService.queryByReq(queryStructReq, User.getFakeUser());
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("访问次数", secondColumn.getName());
        assertEquals(1, result.getResultList().size());
        assertEquals("HR", result.getResultList().get(0).get("department").toString());
    }

    @Test
    public void testCacheQuery() throws Exception {
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("department"));
        QueryStructReq queryStructReq2 = buildQueryStructReq(Arrays.asList("department"));
        SemanticQueryResp result1 = queryService.queryByReq(queryStructReq1, User.getFakeUser());
        SemanticQueryResp result2 = queryService.queryByReq(queryStructReq2, User.getFakeUser());
        assertTrue(result1 == result2);
    }

    @Test
    public void testAuthorization_model() {
        User alice = new User(2L, "alice", "alice", "alice@email", 0);
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("department"));
        assertThrows(InvalidPermissionException.class,
                () -> queryService.queryByReq(queryStructReq1, alice));
    }

    @Test
    public void testAuthorization_sensitive_metric() throws Exception {
        User tom = DataUtils.getUserTom();
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("stay_hours");
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("department"), aggregator);
        SemanticQueryResp semanticQueryResp = queryService.queryByReq(queryStructReq1, tom);
        Assertions.assertEquals(false, semanticQueryResp.getColumns().get(1).getAuthorized());
        Assertions.assertEquals("******", semanticQueryResp.getResultList().get(0).get("stay_hours"));
    }

    @Test
    public void testAuthorization_row_permission() throws Exception {
        User tom = DataUtils.getUserTom();
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("stay_hours");
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("department"), aggregator);
        SemanticQueryResp semanticQueryResp = queryService.queryByReq(queryStructReq1, tom);
        Assertions.assertNotNull(semanticQueryResp.getQueryAuthorization().getMessage());
        Assertions.assertTrue(semanticQueryResp.getSql().contains("`user_name` = 'tom'"));
    }

}
