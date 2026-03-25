package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionContextBuilder;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionOrchestrator;
import com.tencent.supersonic.headless.server.service.impl.ReportScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private ReportScheduleServiceImpl service;
    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        service = new ReportScheduleServiceImpl(quartzJobManager, executionMapper, contextBuilder,
                orchestrator, userService, dataSetAuthService);
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
}
