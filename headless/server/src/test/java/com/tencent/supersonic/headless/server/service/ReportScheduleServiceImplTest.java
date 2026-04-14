package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.ReportExecutionVO;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionContextBuilder;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionOrchestrator;
import com.tencent.supersonic.headless.server.service.impl.ReportScheduleServiceImpl;
import com.tencent.supersonic.headless.server.task.ReportScheduleJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceImplTest {

    @Mock
    private QuartzJobManager quartzJobManager;
    @Mock
    private ReportExecutionMapper executionMapper;
    @Mock
    private ReportExecutionContextBuilder contextBuilder;
    @Mock
    private ReportExecutionOrchestrator orchestrator;
    @Mock
    private UserService userService;
    @Mock
    private DataSetAuthService dataSetAuthService;
    @Mock
    private ReportScheduleMapper reportScheduleMapper;
    @Mock
    private ReportDeliveryRecordMapper deliveryRecordMapper;

    private ReportScheduleServiceImpl service;
    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        service = new ReportScheduleServiceImpl(quartzJobManager, executionMapper, contextBuilder,
                orchestrator, userService, dataSetAuthService, deliveryRecordMapper);
        ReflectionTestUtils.setField(service, "baseMapper", reportScheduleMapper);

        owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        owner.setTenantId(1L);

        other = new User();
        other.setId(99L);
        other.setName("bob");
        other.setTenantId(1L);
    }

    @Test
    void updateScheduleShouldRescheduleUsingExistingQuartzJobKeyWhenCronChanges() {
        ReportScheduleDO existing = new ReportScheduleDO();
        existing.setId(1L);
        existing.setOwnerId(7L);
        existing.setQuartzJobKey("REPORT.report_1");
        existing.setCronExpression("0 0 9 * * ?");
        when(reportScheduleMapper.selectById(1L)).thenReturn(existing);

        when(dataSetAuthService.checkDataSetViewPermission(10L, owner)).thenReturn(true);

        ReportScheduleDO update = new ReportScheduleDO();
        update.setId(1L);
        update.setDatasetId(10L);
        update.setCronExpression("0 30 9 * * ?");

        service.updateSchedule(update, owner);

        verify(reportScheduleMapper).updateById(eq(update));
        verify(quartzJobManager).rescheduleJob("REPORT.report_1", "0 30 9 * * ?");
    }

    @Test
    void updateScheduleShouldRejectDatasetChangeWhenOwnerLacksPermission() {
        ReportScheduleDO existing = new ReportScheduleDO();
        existing.setId(1L);
        existing.setOwnerId(7L);
        existing.setQuartzJobKey("REPORT.report_1");
        existing.setCronExpression("0 0 9 * * ?");
        when(reportScheduleMapper.selectById(1L)).thenReturn(existing);

        when(dataSetAuthService.checkDataSetViewPermission(10L, owner)).thenReturn(false);

        ReportScheduleDO update = new ReportScheduleDO();
        update.setId(1L);
        update.setDatasetId(10L);

        assertThrows(InvalidPermissionException.class, () -> service.updateSchedule(update, owner));
        verify(reportScheduleMapper, never()).updateById(any(ReportScheduleDO.class));
        verify(quartzJobManager, never()).rescheduleJob(any(), any());
    }

    @Test
    void updateScheduleShouldNotRescheduleWhenCronUnchanged() {
        ReportScheduleDO existing = new ReportScheduleDO();
        existing.setId(1L);
        existing.setOwnerId(7L);
        existing.setQuartzJobKey("REPORT.report_1");
        existing.setCronExpression("0 0 9 * * ?");
        when(reportScheduleMapper.selectById(1L)).thenReturn(existing);

        ReportScheduleDO update = new ReportScheduleDO();
        update.setId(1L);
        update.setCronExpression("0 0 9 * * ?"); // same cron

        service.updateSchedule(update, owner);

        verify(reportScheduleMapper).updateById(eq(update));
        verify(quartzJobManager, never()).rescheduleJob(any(), any());
    }

    @Test
    void updateScheduleShouldThrowWhenCallerIsNotOwner() {
        ReportScheduleDO existing = new ReportScheduleDO();
        existing.setId(1L);
        existing.setOwnerId(7L);
        when(reportScheduleMapper.selectById(1L)).thenReturn(existing);

        ReportScheduleDO update = new ReportScheduleDO();
        update.setId(1L);
        update.setCronExpression("0 30 9 * * ?");

        assertThrows(InvalidPermissionException.class, () -> service.updateSchedule(update, other));
        verify(reportScheduleMapper, never())
                .updateById(org.mockito.ArgumentMatchers.<ReportScheduleDO>any());
        verify(quartzJobManager, never()).rescheduleJob(any(), any());
    }

    @Test
    void createScheduleShouldThrowWhenOwnerIdIsNull() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setDatasetId(10L);
        User user = new User();

        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule, user));
        verify(reportScheduleMapper, never()).insert(any(ReportScheduleDO.class));
    }

    @Test
    void createScheduleShouldThrowWhenOwnerUserNotFound() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setDatasetId(10L);
        User user = new User();
        user.setId(99L);
        when(userService.getUserById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule, user));
        verify(reportScheduleMapper, never()).insert(any(ReportScheduleDO.class));
    }

    @Test
    void createScheduleShouldThrowWhenOwnerLacksDatasetPermission() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setDatasetId(10L);

        User owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(10L, owner)).thenReturn(false);

        assertThrows(InvalidPermissionException.class,
                () -> service.createSchedule(schedule, owner));
        verify(reportScheduleMapper, never()).insert(any(ReportScheduleDO.class));
    }

    @Test
    void pauseScheduleShouldThrowWhenCallerIsNotOwner() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey("REPORT.report_1");
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        assertThrows(InvalidPermissionException.class, () -> service.pauseSchedule(1L, other));
        verify(quartzJobManager, never()).pauseJob(any());
    }

    @Test
    void pauseScheduleShouldSucceedForOwner() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey("REPORT.report_1");
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        service.pauseSchedule(1L, owner);

        verify(quartzJobManager).pauseJob("REPORT.report_1");
    }

    @Test
    void deleteScheduleShouldThrowWhenCallerIsNotOwner() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey("REPORT.report_1");
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        assertThrows(InvalidPermissionException.class, () -> service.deleteSchedule(1L, other));
        verify(quartzJobManager, never()).deleteJob(any());
        verify(reportScheduleMapper, never()).deleteById(1L);
    }

    @Test
    void triggerNowShouldThrowWhenCallerIsNotOwner() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey("REPORT.report_1");
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        assertThrows(InvalidPermissionException.class, () -> service.triggerNow(1L, other));
        verify(quartzJobManager, never()).triggerJob(any());
    }

    @Test
    void getScheduleListShouldFilterByOwnerForNonAdmin() {
        service.getScheduleList(new Page<>(1, 20), null, null, owner);

        verify(reportScheduleMapper).selectPage(any(Page.class), any());
    }

    @Test
    void getExecutionByIdShouldThrowWhenCallerIsNotOwner() {
        ReportExecutionDO execution = new ReportExecutionDO();
        execution.setId(11L);
        execution.setScheduleId(1L);
        when(executionMapper.selectById(11L)).thenReturn(execution);

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        assertThrows(InvalidPermissionException.class,
                () -> service.getExecutionById(1L, 11L, other));
    }

    @Test
    void getExecutionByIdShouldReturnNullWhenScheduleIdDoesNotMatch() {
        ReportExecutionDO execution = new ReportExecutionDO();
        execution.setId(11L);
        execution.setScheduleId(2L);
        when(executionMapper.selectById(11L)).thenReturn(execution);

        assertNull(service.getExecutionById(1L, 11L, owner));
    }

    @Test
    void executeReportShouldThrowWhenCallerIsNotOwner() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        assertThrows(InvalidPermissionException.class, () -> service.executeReport(1L, other));
        verify(contextBuilder, never()).buildManualFromSchedule(any(), any());
        verify(orchestrator, never()).execute(any());
    }

    @Test
    void executeReportShouldUseOwnerAfterPermissionCheck() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);

        service.executeReport(1L, owner);

        verify(contextBuilder).buildManualFromSchedule(schedule, owner);
        verify(orchestrator).execute(any());
    }

    // --- Quartz recovery tests ---

    @Test
    void triggerNowShouldNormaliseNullDbKeyWhenQuartzJobExists() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey(null); // DB key is null
        schedule.setCronExpression("0 0 9 * * ?");
        schedule.setTenantId(1L);
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(quartzJobManager.jobExists("REPORT.report_1")).thenReturn(true);

        service.triggerNow(1L, owner);

        // Key should be normalised in DB
        ArgumentCaptor<ReportScheduleDO> captor = ArgumentCaptor.forClass(ReportScheduleDO.class);
        verify(reportScheduleMapper).updateById(captor.capture());
        assertEquals("REPORT.report_1", captor.getValue().getQuartzJobKey());
        // Should trigger with the correct key, not null (implementation passes manual flag map)
        verify(quartzJobManager).triggerJob(eq("REPORT.report_1"), any(JobDataMap.class));
        // Should NOT recreate since job already exists
        verify(quartzJobManager, never()).recreateJob(anyString(), anyString(), anyLong(), any(),
                anyString(), any(JobDataMap.class));
    }

    @Test
    void triggerNowShouldRecreateJobWhenQuartzJobMissing() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(1L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey("REPORT.report_1");
        schedule.setCronExpression("0 0 9 * * ?");
        schedule.setTenantId(1L);
        when(reportScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(quartzJobManager.jobExists("REPORT.report_1")).thenReturn(false);
        when(quartzJobManager.recreateJob(eq("REPORT"), eq("report_"), eq(1L),
                eq(ReportScheduleJob.class), eq("0 0 9 * * ?"), any(JobDataMap.class)))
                        .thenReturn("REPORT.report_1");

        service.triggerNow(1L, owner);

        // recreateJob should be called to clean up orphan triggers and rebuild
        verify(quartzJobManager).recreateJob(eq("REPORT"), eq("report_"), eq(1L),
                eq(ReportScheduleJob.class), eq("0 0 9 * * ?"), any(JobDataMap.class));
        verify(quartzJobManager).triggerJob(eq("REPORT.report_1"), any(JobDataMap.class));
    }

    @Test
    void startupRecoveryShouldReRegisterMissingQuartzJobs() {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(5L);
        schedule.setOwnerId(7L);
        schedule.setQuartzJobKey(null); // never registered
        schedule.setCronExpression("0 0 10 * * ?");
        schedule.setTenantId(1L);
        schedule.setEnabled(true);

        when(reportScheduleMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(reportScheduleMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(schedule));
        when(quartzJobManager.jobExists("REPORT.report_5")).thenReturn(false);
        when(quartzJobManager.recreateJob(eq("REPORT"), eq("report_"), eq(5L),
                eq(ReportScheduleJob.class), eq("0 0 10 * * ?"), any(JobDataMap.class)))
                        .thenReturn("REPORT.report_5");

        service.recoverAndWarnOnStartup();

        // Should recreate the missing job
        verify(quartzJobManager).recreateJob(eq("REPORT"), eq("report_"), eq(5L),
                eq(ReportScheduleJob.class), eq("0 0 10 * * ?"), any(JobDataMap.class));
        // Should update DB with the canonical key
        ArgumentCaptor<ReportScheduleDO> captor = ArgumentCaptor.forClass(ReportScheduleDO.class);
        verify(reportScheduleMapper).updateById(captor.capture());
        assertEquals("REPORT.report_5", captor.getValue().getQuartzJobKey());
    }

    // ── Phase 2: queryConfig validation tests ──────────────────────────

    @Test
    void createScheduleShouldRejectBetweenWithoutDateField() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        ReportScheduleDO schedule = buildScheduleWithQueryConfig(
                "{\"dateInfo\":{\"dateMode\":\"BETWEEN\",\"startDate\":\"2025-03-04\",\"endDate\":\"2025-03-10\"}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSchedule(schedule, owner));
        assertEquals("请选择或填写日期字段", ex.getMessage());
    }

    @Test
    void createScheduleShouldRejectBetweenWithoutStartDate() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        // DateConf.startDate has a default value, so we must explicitly null it
        ReportScheduleDO schedule = buildScheduleWithQueryConfig(
                "{\"dateInfo\":{\"dateMode\":\"BETWEEN\",\"dateField\":\"workday\",\"startDate\":null,\"endDate\":\"2025-03-10\"}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSchedule(schedule, owner));
        assertEquals("请选择日期范围", ex.getMessage());
    }

    @Test
    void createScheduleShouldRejectRecentWithoutDateField() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        ReportScheduleDO schedule = buildScheduleWithQueryConfig(
                "{\"dateInfo\":{\"dateMode\":\"RECENT\",\"unit\":7,\"period\":\"DAY\"}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSchedule(schedule, owner));
        assertEquals("请选择或填写日期字段", ex.getMessage());
    }

    @Test
    void createScheduleShouldRejectAllModeForDetailQuery() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        ReportScheduleDO schedule = buildScheduleWithQueryConfig(
                "{\"dateInfo\":{\"dateMode\":\"ALL\",\"dateField\":\"workday\"},\"queryType\":\"DETAIL\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSchedule(schedule, owner));
        assertEquals("明细调度不支持 ALL 模式，请选择固定区间或最近 N 天", ex.getMessage());
    }

    @Test
    void createScheduleShouldAcceptValidBetweenConfig() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        ReportScheduleDO schedule = buildScheduleWithQueryConfig(
                "{\"queryType\":\"DETAIL\",\"dimensions\":[{\"name\":\"workday\",\"bizName\":\"workday\"},"
                        + "{\"name\":\"order_id\",\"bizName\":\"order_id\"}],"
                        + "\"groups\":[],\"limit\":500,"
                        + "\"dateInfo\":{\"dateMode\":\"BETWEEN\",\"dateField\":\"workday\","
                        + "\"startDate\":\"2025-03-04\",\"endDate\":\"2025-03-10\"}}");

        service.createSchedule(schedule, owner);
        verify(reportScheduleMapper).insert(any(ReportScheduleDO.class));
    }

    @Test
    void createScheduleShouldRejectDetailWithoutDimensions() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        ReportScheduleDO schedule =
                buildScheduleWithQueryConfig("{\"queryType\":\"DETAIL\",\"limit\":500,"
                        + "\"dateInfo\":{\"dateMode\":\"BETWEEN\",\"dateField\":\"workday\","
                        + "\"startDate\":\"2025-03-04\",\"endDate\":\"2025-03-10\"}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSchedule(schedule, owner));
        assertEquals("明细调度需要至少一个查询列", ex.getMessage());
    }

    @Test
    void createScheduleShouldRejectDetailWithZeroLimit() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        // limit=0 should be rejected; omitting limit defaults to 500 which is valid
        ReportScheduleDO schedule = buildScheduleWithQueryConfig(
                "{\"queryType\":\"DETAIL\",\"dimensions\":[{\"name\":\"workday\",\"bizName\":\"workday\"}],"
                        + "\"groups\":[\"workday\"],\"limit\":0,"
                        + "\"dateInfo\":{\"dateMode\":\"BETWEEN\",\"dateField\":\"workday\","
                        + "\"startDate\":\"2025-03-04\",\"endDate\":\"2025-03-10\"}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createSchedule(schedule, owner));
        assertEquals("明细调度需要有效的 limit", ex.getMessage());
    }

    @Test
    void createScheduleShouldAcceptBlankQueryConfig() {
        when(userService.getUserById(7L)).thenReturn(owner);
        when(dataSetAuthService.checkDataSetViewPermission(anyLong(), any())).thenReturn(true);

        ReportScheduleDO schedule = buildScheduleWithQueryConfig(null);

        service.createSchedule(schedule, owner);
        verify(reportScheduleMapper).insert(any(ReportScheduleDO.class));
    }

    @Test
    void updateScheduleShouldRejectInvalidQueryConfigBeforePersist() {
        ReportScheduleDO existing = new ReportScheduleDO();
        existing.setId(1L);
        existing.setOwnerId(7L);
        existing.setQuartzJobKey("REPORT.report_1");
        existing.setCronExpression("0 0 9 * * ?");
        when(reportScheduleMapper.selectById(1L)).thenReturn(existing);

        ReportScheduleDO update = new ReportScheduleDO();
        update.setId(1L);
        update.setQueryConfig("{\"queryType\":\"DETAIL\",\"limit\":500,"
                + "\"dateInfo\":{\"dateMode\":\"BETWEEN\",\"dateField\":\"workday\","
                + "\"startDate\":\"2025-03-04\",\"endDate\":\"2025-03-10\"}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateSchedule(update, owner));
        assertEquals("明细调度需要至少一个查询列", ex.getMessage());
        verify(reportScheduleMapper, never()).updateById(any(ReportScheduleDO.class));
        verify(quartzJobManager, never()).rescheduleJob(any(), any());
    }

    private ReportScheduleDO buildScheduleWithQueryConfig(String queryConfig) {
        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setOwnerId(7L);
        schedule.setDatasetId(8L);
        schedule.setCronExpression("0 30 14 * * ?");
        schedule.setTenantId(1L);
        schedule.setQueryConfig(queryConfig);
        return schedule;
    }

    @Test
    void getExecutionVOListShouldRollupDeliveryAcrossChannels() {
        User admin = new User();
        admin.setId(1L);
        admin.setName("admin");
        admin.setTenantId(1L);
        admin.setIsAdmin(1);

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(50L);
        schedule.setOwnerId(1L);
        when(reportScheduleMapper.selectById(50L)).thenReturn(schedule);

        ReportExecutionDO e1 = execution(101L, 50L);
        ReportExecutionDO e2 = execution(102L, 50L);
        ReportExecutionDO e3 = execution(103L, 50L);
        ReportExecutionDO e4 = execution(104L, 50L);

        Page<ReportExecutionDO> doPage = new Page<>(1, 20);
        doPage.setRecords(List.of(e1, e2, e3, e4));
        doPage.setTotal(4);
        when(executionMapper.selectPage(any(Page.class), any())).thenReturn(doPage);

        List<ReportDeliveryRecordDO> allRecords = List.of(
                // e1: 两个渠道都成功
                record(101L, 1L, "EMAIL", DeliveryStatus.SUCCESS),
                record(101L, 2L, "FEISHU", DeliveryStatus.SUCCESS),
                // e2: 邮件成功 + 飞书失败 → PARTIAL
                record(102L, 1L, "EMAIL", DeliveryStatus.SUCCESS),
                record(102L, 2L, "FEISHU", DeliveryStatus.FAILED),
                // e3: 两个渠道都失败 → FAILED
                record(103L, 1L, "EMAIL", DeliveryStatus.FAILED),
                record(103L, 2L, "FEISHU", DeliveryStatus.FAILED),
                // e4: 一个 PENDING + 一个 SUCCESS → IN_PROGRESS
                record(104L, 1L, "EMAIL", DeliveryStatus.SUCCESS),
                record(104L, 2L, "FEISHU", DeliveryStatus.PENDING));
        when(deliveryRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(allRecords);

        Page<ReportExecutionVO> result =
                service.getExecutionVOList(new Page<>(1, 20), 50L, null, admin);

        List<ReportExecutionVO> rows = result.getRecords();
        assertEquals(4, rows.size());
        assertRollup(rows.get(0), List.of("EMAIL", "FEISHU"), "DELIVERED", 2, 2);
        assertRollup(rows.get(1), List.of("EMAIL", "FEISHU"), "PARTIAL", 1, 2);
        assertRollup(rows.get(2), List.of("EMAIL", "FEISHU"), "FAILED", 0, 2);
        assertRollup(rows.get(3), List.of("EMAIL", "FEISHU"), "IN_PROGRESS", 1, 2);
    }

    @Test
    void getExecutionVOListShouldReturnNoneRollupWhenNoDeliveryRecords() {
        User admin = new User();
        admin.setId(1L);
        admin.setName("admin");
        admin.setTenantId(1L);
        admin.setIsAdmin(1);

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(50L);
        schedule.setOwnerId(1L);
        when(reportScheduleMapper.selectById(50L)).thenReturn(schedule);

        ReportExecutionDO e1 = execution(200L, 50L);
        Page<ReportExecutionDO> doPage = new Page<>(1, 20);
        doPage.setRecords(List.of(e1));
        doPage.setTotal(1);
        when(executionMapper.selectPage(any(Page.class), any())).thenReturn(doPage);
        when(deliveryRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        Page<ReportExecutionVO> result =
                service.getExecutionVOList(new Page<>(1, 20), 50L, null, admin);

        ReportExecutionVO vo = result.getRecords().get(0);
        assertEquals("NONE", vo.getDeliveryRollup());
        assertEquals(0, vo.getDeliveryTotalCount());
        assertEquals(0, vo.getDeliverySuccessCount());
        assertEquals(List.of(), vo.getChannelTypes());
    }

    @Test
    void getExecutionVOListShouldKeepLatestPerConfigIgnoringRetryHistory() {
        User admin = new User();
        admin.setId(1L);
        admin.setName("admin");
        admin.setTenantId(1L);
        admin.setIsAdmin(1);

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setId(50L);
        schedule.setOwnerId(1L);
        when(reportScheduleMapper.selectById(50L)).thenReturn(schedule);

        ReportExecutionDO e1 = execution(300L, 50L);
        Page<ReportExecutionDO> doPage = new Page<>(1, 20);
        doPage.setRecords(List.of(e1));
        doPage.setTotal(1);
        when(executionMapper.selectPage(any(Page.class), any())).thenReturn(doPage);

        // 同一 configId=1 先失败再重试成功——rollup 只看最新那条（SUCCESS），不能按记录数 2/2 算。
        ReportDeliveryRecordDO oldFailed = record(300L, 1L, "EMAIL", DeliveryStatus.FAILED);
        oldFailed.setCompletedAt(new Date(1_000L));
        ReportDeliveryRecordDO retrySuccess = record(300L, 1L, "EMAIL", DeliveryStatus.SUCCESS);
        retrySuccess.setCompletedAt(new Date(2_000L));
        when(deliveryRecordMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(oldFailed, retrySuccess));

        Page<ReportExecutionVO> result =
                service.getExecutionVOList(new Page<>(1, 20), 50L, null, admin);
        ReportExecutionVO vo = result.getRecords().get(0);
        assertEquals("DELIVERED", vo.getDeliveryRollup());
        assertEquals(1, vo.getDeliveryTotalCount());
        assertEquals(1, vo.getDeliverySuccessCount());
    }

    private ReportExecutionDO execution(long id, long scheduleId) {
        ReportExecutionDO execution = new ReportExecutionDO();
        execution.setId(id);
        execution.setScheduleId(scheduleId);
        execution.setStatus("SUCCESS");
        return execution;
    }

    private ReportDeliveryRecordDO record(long executionId, long configId, String type,
            DeliveryStatus status) {
        ReportDeliveryRecordDO record = new ReportDeliveryRecordDO();
        record.setExecutionId(executionId);
        record.setConfigId(configId);
        record.setDeliveryType(type);
        record.setStatus(status.name());
        record.setCreatedAt(new Date());
        return record;
    }

    private void assertRollup(ReportExecutionVO vo, List<String> expectedChannels,
            String expectedRollup, int expectedSuccess, int expectedTotal) {
        assertEquals(expectedRollup, vo.getDeliveryRollup());
        assertEquals(expectedTotal, vo.getDeliveryTotalCount());
        assertEquals(expectedSuccess, vo.getDeliverySuccessCount());
        // channelTypes 顺序按 LinkedHashSet 保留首次出现顺序
        assertEquals(expectedChannels.size(), vo.getChannelTypes().size());
        assertEquals(new java.util.HashSet<>(expectedChannels),
                new java.util.HashSet<>(vo.getChannelTypes()));
    }
}
