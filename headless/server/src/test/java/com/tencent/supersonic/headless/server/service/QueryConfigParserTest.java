package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DetailTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SqlTemplateConfig;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.service.DataSetService;
import com.tencent.supersonic.headless.core.utils.SqlTemplateEngine;
import com.tencent.supersonic.headless.server.service.impl.QueryConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryConfigParserTest {

    @Mock
    private SqlTemplateEngine sqlTemplateEngine;
    @Mock
    private DataSetService dataSetService;

    @InjectMocks
    private QueryConfigParser queryConfigParser;

    // ---- Test 1: QueryStructReq returned directly (not converted to QuerySqlReq) ----

    @Test
    void parse_queryStructReqJson_returnsQueryStructReqInstance() {
        QueryStructReq input = new QueryStructReq();
        input.setDataSetId(42L);
        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, null, null);

        assertInstanceOf(QueryStructReq.class, result,
                "QueryStructReq config should be returned as QueryStructReq, not converted to QuerySqlReq");
    }

    // ---- Test 2: BETWEEN dateInfo preserved without downgrade ----

    @Test
    void parse_betweenDateInfo_preservedExactly() {
        QueryStructReq input = new QueryStructReq();
        input.setDataSetId(10L);

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.BETWEEN);
        dateConf.setStartDate("2025-03-04");
        dateConf.setEndDate("2025-03-10");
        dateConf.setDateField("workday");
        input.setDateInfo(dateConf);

        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, null, null);

        assertInstanceOf(QueryStructReq.class, result);
        QueryStructReq structResult = (QueryStructReq) result;

        assertNotNull(structResult.getDateInfo(), "dateInfo should not be null");
        assertEquals(DateConf.DateMode.BETWEEN, structResult.getDateInfo().getDateMode());
        assertEquals("2025-03-04", structResult.getDateInfo().getStartDate());
        assertEquals("2025-03-10", structResult.getDateInfo().getEndDate());
        assertEquals("workday", structResult.getDateInfo().getDateField());
    }

    // ---- Test 3: DatasetId injection when config does not carry one ----

    @Test
    void parse_injectsDatasetId_whenConfigHasNone() {
        QueryStructReq input = new QueryStructReq();
        // Do NOT set dataSetId on input — it should be null
        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, 99L, null);

        assertInstanceOf(QueryStructReq.class, result);
        assertEquals(99L, result.getDataSetId());
    }

    @Test
    void parse_doesNotOverrideExistingDatasetId() {
        QueryStructReq input = new QueryStructReq();
        input.setDataSetId(5L);
        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, 99L, null);

        assertInstanceOf(QueryStructReq.class, result);
        assertEquals(5L, result.getDataSetId(),
                "Existing dataSetId in config should not be overridden");
    }

    // ---- Test 4: Blank / null queryConfig throws IllegalArgumentException ----

    @Test
    void parse_nullQueryConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> queryConfigParser.parse(null, 1L, null));
    }

    @Test
    void parse_emptyQueryConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> queryConfigParser.parse("", 1L, null));
    }

    @Test
    void parse_blankQueryConfig_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> queryConfigParser.parse("   ", 1L, null));
    }

    // ---- Test 5: QuerySqlReq fallback ----
    // The parser tries QueryStructReq (Path 2) before QuerySqlReq (Path 3).
    // Jackson ignores unknown fields, so a QuerySqlReq JSON also deserializes as
    // QueryStructReq. Path 2 returns when dataSetId is non-null. Therefore the
    // QuerySqlReq path is only reachable when the JSON carries no dataSetId AND
    // the external datasetId parameter is also null (so Path 2 falls through).

    @Test
    void parse_querySqlReqJson_fallsToQuerySqlReq_whenNoDatasetId() {
        // Build a QuerySqlReq JSON *without* dataSetId
        QuerySqlReq input = QuerySqlReq.builder()
                .sql("SELECT city, SUM(gmv) FROM orders GROUP BY city").build();
        // dataSetId intentionally left null
        String json = JsonUtil.toString(input);

        // Pass null external datasetId so Path 2 (QueryStructReq) falls through
        SemanticQueryReq result = queryConfigParser.parse(json, null, null);

        assertInstanceOf(QuerySqlReq.class, result,
                "Config with sql field and no dataSetId should fall through to QuerySqlReq path");
        QuerySqlReq sqlResult = (QuerySqlReq) result;
        assertEquals("SELECT city, SUM(gmv) FROM orders GROUP BY city", sqlResult.getSql());
    }

    @Test
    void parse_querySqlReqJson_injectsDatasetId_inSqlPath() {
        QuerySqlReq input = QuerySqlReq.builder().sql("SELECT 1").build();
        // dataSetId intentionally left null in JSON
        String json = JsonUtil.toString(input);

        // External datasetId is non-null; however Path 2 (QueryStructReq) also
        // receives it, so it returns as QueryStructReq. This is expected: if a
        // datasetId is available, the parser prefers the QueryStructReq path.
        SemanticQueryReq result = queryConfigParser.parse(json, 55L, null);

        // With external datasetId, Path 2 succeeds first
        assertInstanceOf(QueryStructReq.class, result);
        assertEquals(55L, result.getDataSetId());
    }

    @Test
    void parse_querySqlReqJson_withDatasetIdInJson_returnsAsQueryStructReq() {
        // When QuerySqlReq JSON carries a dataSetId, Path 2 (QueryStructReq)
        // succeeds first because Jackson ignores the unknown "sql" field.
        QuerySqlReq input = QuerySqlReq.builder().sql("SELECT city FROM orders").build();
        input.setDataSetId(7L);
        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, null, null);

        // Path 2 wins — this documents the actual behavior
        assertInstanceOf(QueryStructReq.class, result);
        assertEquals(7L, result.getDataSetId());
    }

    // ---- Test: resolvedParams passed as null does not cause NPE ----

    @Test
    void parse_nullResolvedParams_doesNotThrow() {
        QueryStructReq input = new QueryStructReq();
        input.setDataSetId(1L);
        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, null, null);

        assertNotNull(result);
    }

    // ---- Test: resolvedParams passed as empty map does not cause NPE ----

    @Test
    void parse_emptyResolvedParams_doesNotThrow() {
        QueryStructReq input = new QueryStructReq();
        input.setDataSetId(1L);
        String json = JsonUtil.toString(input);

        SemanticQueryReq result = queryConfigParser.parse(json, null, Map.of());

        assertNotNull(result);
    }

    @Test
    void parseForAlert_sqlTemplate_usesDatasetDetailLimit() {
        SqlTemplateConfig templateConfig = new SqlTemplateConfig();
        templateConfig.setTemplateSql("SELECT * FROM t");
        String json = JsonUtil.toString(templateConfig);
        when(sqlTemplateEngine.render("SELECT * FROM t", Map.of())).thenReturn("SELECT * FROM t");

        DetailTypeDefaultConfig detailConfig = new DetailTypeDefaultConfig();
        detailConfig.setLimit(88);
        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setDetailTypeDefaultConfig(detailConfig);
        DataSetResp dataSet = new DataSetResp();
        dataSet.setQueryConfig(queryConfig);
        when(dataSetService.getDataSet(1L)).thenReturn(dataSet);

        SemanticQueryReq result = queryConfigParser.parseForAlert(json, 1L);

        assertInstanceOf(QuerySqlReq.class, result);
        assertEquals("SELECT * FROM (SELECT * FROM t) AS _alert_query_limit LIMIT 88",
                ((QuerySqlReq) result).getSql());
    }
}
