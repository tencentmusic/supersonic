package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.api.service.DataSetService;
import com.tencent.supersonic.headless.api.service.ReportDeliveryService;
import com.tencent.supersonic.headless.api.service.ReportScheduleConfirmationService;
import com.tencent.supersonic.headless.api.service.ReportScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportScheduleQuery — intent handling and two-step confirmation flow")
class ReportScheduleQueryTest {

    @Mock
    private ReportScheduleService scheduleService;
    @Mock
    private ReportScheduleConfirmationService confirmationService;
    @Mock
    private ChatManageService chatManageService;
    @Mock
    private DataSetService dataSetService;
    @Mock
    private ReportDeliveryService deliveryService;
    @Mock
    private UserService userService;

    private ReportScheduleQuery query;

    @BeforeEach
    void setUp() {
        query = new ReportScheduleQuery(scheduleService, confirmationService, chatManageService,
                dataSetService, deliveryService, userService);
        org.springframework.test.util.ReflectionTestUtils.setField(query, "confirmationExpireMs",
                300_000L);
        lenient().when(userService.getUserById(anyLong()))
                .thenAnswer(inv -> buildUser(inv.getArgument(0), "tester", 10L));
    }

    // ── Anti-Goal AG-01: cross-tenant isolation ─────────────────────────

    @Test
    @DisplayName("AG-01: getScheduleById returns null for cross-tenant id → ERROR_SCHEDULE_NOT_FOUND")
    void cancelWithUnknownId_returnsNotFound() {
        when(scheduleService.getScheduleById(eq(999L), any())).thenReturn(null);

        query.setParseInfo(buildParseInfo("取消报表 #999", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("#999"));
    }

    @Test
    @DisplayName("AG-01: pause on cross-tenant id (null return) → error, not silent skip")
    void pauseWithUnknownId_returnsNotFound() {
        when(scheduleService.getScheduleById(eq(42L), any())).thenReturn(null);

        query.setParseInfo(buildParseInfo("暂停报表 #42", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("#42"));
    }

    // ── CREATE intent — validation paths ──────────────────────────────

    @Test
    @DisplayName("CREATE without frequency → ERROR_SPECIFY_FREQUENCY, no confirmation saved")
    void createWithoutFrequency_returnsError() {
        query.setParseInfo(buildParseInfo("基于刚才那个报表，推给我", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_SPECIFY_FREQUENCY, resp.getMessage());
        verify(confirmationService, never()).createPending(any());
    }

    @Test
    @DisplayName("CREATE without prior schedulable query → ERROR_SPECIFY_REPORT_CONTENT")
    void createWithoutPreviousQuery_returnsError() {
        when(chatManageService.getChatQueries(anyInt())).thenReturn(List.of());

        query.setParseInfo(buildParseInfo("每天9点发给我", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_SPECIFY_REPORT_CONTENT, resp.getMessage());
        verify(confirmationService, never()).createPending(any());
    }

    @Test
    @DisplayName("CREATE without delivery config → ERROR_NO_DELIVERY_CONFIG")
    void createWithoutDeliveryConfig_returnsError() {
        when(chatManageService.getChatQueries(anyInt()))
                .thenReturn(List.of(buildPreviousQuery(5001L, buildSchedulableParseInfo(1))));

        Page<ReportDeliveryConfigResp> emptyPage = new Page<>(1, 100);
        emptyPage.setRecords(List.of());
        when(deliveryService.getConfigList(any())).thenReturn(emptyPage);

        query.setParseInfo(buildParseInfo("每天9点发给我", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_NO_DELIVERY_CONFIG, resp.getMessage());
    }

    // ── CREATE → CONFIRM two-step flow ────────────────────────────────

    @Test
    @DisplayName("CREATE builds pending confirmation; CONFIRM executes and returns schedule id")
    void createThenConfirm_createsSchedule() {
        when(chatManageService.getChatQueries(anyInt()))
                .thenReturn(List.of(buildPreviousQuery(5001L, buildSchedulableParseInfo(1))));

        ReportDeliveryConfigResp config = new ReportDeliveryConfigResp();
        config.setId(9L);
        config.setName("飞书群");
        config.setEnabled(true);
        config.setTenantId(10L);
        Page<ReportDeliveryConfigResp> configPage = new Page<>(1, 100);
        configPage.setRecords(List.of(config));
        when(deliveryService.getConfigList(any())).thenReturn(configPage);
        when(deliveryService.getConfigById(9L)).thenReturn(config);

        DataSetResp ds = new DataSetResp();
        ds.setId(1L);
        ds.setName("订单明细");
        when(dataSetService.getDataSet(1L)).thenReturn(ds);

        ReportScheduleConfirmationResp[] stored = {null};
        doAnswer(inv -> {
            ReportScheduleConfirmationReq captured = inv.getArgument(0);
            ReportScheduleConfirmationResp c = new ReportScheduleConfirmationResp();
            BeanUtils.copyProperties(captured, c);
            c.setId(1L);
            c.setStatus("PENDING");
            stored[0] = c;
            return c;
        }).when(confirmationService).createPending(any());

        // Act: CREATE
        query.setParseInfo(buildParseInfo("基于刚才那个报表，每天10:30推送给我", 88, 6001L, 1001L, "tester", 10L));
        QueryResult createResult = query.build();
        ReportScheduleResp createResp = (ReportScheduleResp) createResult.getResponse();

        assertTrue(createResp.isSuccess());
        assertTrue(createResp.isNeedConfirm());
        assertEquals("0 30 10 * * ?", createResp.getCronExpression());
        verify(confirmationService).createPending(any());

        // Arrange: confirmation lookup
        when(confirmationService.getLatestPending(1001L, 88)).thenReturn(stored[0]);
        when(userService.getUserById(1001L)).thenReturn(buildUser(1001L, "tester", 10L));

        com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp created =
                new com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp();
        created.setId(123L);
        when(scheduleService.createSchedule(any(), any())).thenReturn(created);

        // Act: CONFIRM
        query.setParseInfo(buildParseInfo("确认", 88, 6002L, 1001L, "tester", 10L));
        QueryResult confirmResult = query.build();
        ReportScheduleResp confirmResp = (ReportScheduleResp) confirmResult.getResponse();

        assertTrue(confirmResp.isSuccess());
        assertEquals(123L, confirmResp.getScheduleId());
        verify(scheduleService).createSchedule(any(), any());
        verify(scheduleService, never()).triggerNow(any(), any());
    }

    @Test
    @DisplayName("CREATE with triggerNow phrase → confirm triggers immediate execution")
    void createWithTriggerNow_triggersAfterConfirm() {
        when(chatManageService.getChatQueries(anyInt()))
                .thenReturn(List.of(buildPreviousQuery(5001L, buildSchedulableParseInfo(1))));

        ReportDeliveryConfigResp config = new ReportDeliveryConfigResp();
        config.setId(9L);
        config.setName("飞书群");
        config.setEnabled(true);
        config.setTenantId(10L);
        Page<ReportDeliveryConfigResp> configPage = new Page<>(1, 100);
        configPage.setRecords(List.of(config));
        when(deliveryService.getConfigList(any())).thenReturn(configPage);
        when(deliveryService.getConfigById(9L)).thenReturn(config);

        DataSetResp ds = new DataSetResp();
        ds.setId(1L);
        ds.setName("订单");
        when(dataSetService.getDataSet(1L)).thenReturn(ds);

        ReportScheduleConfirmationResp[] stored = {null};
        doAnswer(inv -> {
            ReportScheduleConfirmationReq captured = inv.getArgument(0);
            ReportScheduleConfirmationResp c = new ReportScheduleConfirmationResp();
            BeanUtils.copyProperties(captured, c);
            c.setId(2L);
            c.setStatus("PENDING");
            stored[0] = c;
            return c;
        }).when(confirmationService).createPending(any());

        query.setParseInfo(
                buildParseInfo("基于刚才那个报表，每天10:30推送，现在先发一次", 88, 6001L, 1001L, "tester", 10L));
        QueryResult createResult = query.build();
        ReportScheduleResp createResp = (ReportScheduleResp) createResult.getResponse();

        assertTrue(
                Boolean.TRUE.equals(createResp.getConfirmAction().getParams().get("triggerNow")));

        when(confirmationService.getLatestPending(1001L, 88)).thenReturn(stored[0]);
        when(userService.getUserById(1001L)).thenReturn(buildUser(1001L, "tester", 10L));
        com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp created =
                new com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp();
        created.setId(124L);
        when(scheduleService.createSchedule(any(), any())).thenReturn(created);

        query.setParseInfo(buildParseInfo("确认", 88, 6002L, 1001L, "tester", 10L));
        QueryResult confirmResult = query.build();
        ReportScheduleResp confirmResp = (ReportScheduleResp) confirmResult.getResponse();

        assertTrue(confirmResp.isSuccess());
        verify(scheduleService).triggerNow(eq(124L), any());
        assertTrue(confirmResp.getMessage().contains("已触发一次立即执行"));
    }

    @Test
    @DisplayName("CONFIRM with no pending → ERROR_NO_PENDING")
    void confirmWithNoPending_returnsError() {
        when(confirmationService.getLatestPending(1L, 1)).thenReturn(null);

        query.setParseInfo(buildParseInfo("确认", 1, 200L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_NO_PENDING, resp.getMessage());
    }

    // ── LIST ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LIST with no schedules → LIST_EMPTY message")
    void list_empty() {
        Page<com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp> emptyPage =
                new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        when(scheduleService.getScheduleList(any(), isNull(), isNull(), any()))
                .thenReturn(emptyPage);

        query.setParseInfo(buildParseInfo("我的定时报表有哪些", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertTrue(resp.isSuccess());
        assertEquals(ScheduleMessages.LIST_EMPTY, resp.getMessage());
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private SemanticParseInfo buildParseInfo(String queryText, Integer chatId, Long queryId,
            Long userId, String userName, Long tenantId) {
        PluginParseResult pr = new PluginParseResult();
        pr.setQueryText(queryText);
        pr.setChatId(chatId);
        pr.setQueryId(queryId);
        pr.setUserId(userId);
        pr.setUserName(userName);
        pr.setTenantId(tenantId);

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode(ReportScheduleQuery.QUERY_MODE);
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.CONTEXT, pr);
        parseInfo.setProperties(props);
        return parseInfo;
    }

    private QueryResp buildPreviousQuery(Long questionId, SemanticParseInfo parseInfo) {
        QueryResp q = new QueryResp();
        q.setQuestionId(questionId);
        q.setQueryText("历史查询");
        q.setParseInfos(List.of(parseInfo));
        return q;
    }

    private SemanticParseInfo buildSchedulableParseInfo(int id) {
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSetId(1L);
        dataSet.setName("订单明细");
        dataSet.setBizName("order_detail");

        SchemaElement metric = new SchemaElement();
        metric.setName("订单数");
        metric.setBizName("order_cnt");
        metric.setDefaultAgg("COUNT");

        SemanticParseInfo pi = new SemanticParseInfo();
        pi.setId(id);
        pi.setQueryType(QueryType.AGGREGATE);
        pi.setAggType(AggregateTypeEnum.NONE);
        pi.setDataSet(dataSet);
        pi.getMetrics().add(metric);
        return pi;
    }

    private User buildUser(Long id, String name, Long tenantId) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setTenantId(tenantId);
        return user;
    }
}
