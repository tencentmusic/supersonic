package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.service.DataSetService;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import com.tencent.supersonic.headless.server.service.impl.ExportTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTaskServiceImplTest {

    @Test
    void submitExportTaskShouldRejectBlankQueryConfig() {
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        ExportTaskMapper mapper = mock(ExportTaskMapper.class);
        UserService userService = mock(UserService.class);
        DataSetService dataSetService = mock(DataSetService.class);
        ExportTaskServiceImpl service =
                new ExportTaskServiceImpl(executor, null, null, userService, dataSetService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        ExportTaskDO task = new ExportTaskDO();
        task.setDatasetId(1L);
        task.setOutputFormat("EXCEL");

        assertThrows(IllegalArgumentException.class, () -> service.submitExportTask(task));
    }

    @Test
    void submitExportTaskShouldFillDefaultTaskNameWhenMissing() {
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        ExportTaskMapper mapper = mock(ExportTaskMapper.class);
        UserService userService = mock(UserService.class);
        DataSetService dataSetService = mock(DataSetService.class);
        DataSetResp ds = new DataSetResp();
        ds.setId(8L);
        ds.setName("示例数据集");
        when(dataSetService.getDataSet(8L)).thenReturn(ds);
        ExportTaskServiceImpl service =
                new ExportTaskServiceImpl(executor, null, null, userService, dataSetService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        ExportTaskDO task = new ExportTaskDO();
        task.setId(1L);
        task.setDatasetId(8L);
        task.setQueryConfig("{\"dataSetId\":8,\"sql\":\"select 1\"}");
        task.setOutputFormat("EXCEL");
        when(mapper.insert(task)).thenReturn(1);

        service.submitExportTask(task);

        assertTrue(task.getTaskName() != null && task.getTaskName().startsWith("数据导出_示例数据集_"));
        verify(mapper).insert(task);
    }

    @Test
    void shouldExecuteAsyncWhenEstimateExceedsThreshold() {
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        SemanticLayerService semanticLayerService = mock(SemanticLayerService.class);
        RowCountEstimator rowCountEstimator = mock(RowCountEstimator.class);
        when(rowCountEstimator.estimate(1L, "SELECT 1")).thenReturn(50_001L);
        ExportTaskServiceImpl service = new ExportTaskServiceImpl(executor, semanticLayerService,
                rowCountEstimator, mock(UserService.class), mock(DataSetService.class));
        ReflectionTestUtils.setField(service, "asyncThreshold", 50_000L);
        assertTrue(service.shouldExecuteAsync(1L, "SELECT 1"));
    }

    @Test
    void executeExportShouldSetFailedAndErrorMessageWhenQueryThrows(@TempDir Path exportDir)
            throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        SemanticLayerService semanticLayerService = mock(SemanticLayerService.class);
        when(semanticLayerService.queryByReq(any(), any())).thenThrow(new SQLException("模拟数据源不可用"));

        ExportTaskMapper mapper = mock(ExportTaskMapper.class);
        UserService userService = mock(UserService.class);
        DataSetService dataSetService = mock(DataSetService.class);
        ExportTaskServiceImpl service = new ExportTaskServiceImpl(executor, semanticLayerService,
                mock(RowCountEstimator.class), userService, dataSetService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "exportDir", exportDir.toString());

        ExportTaskDO task = new ExportTaskDO();
        task.setId(7L);
        task.setUserId(1L);
        task.setTenantId(1L);
        task.setDatasetId(8L);
        task.setTaskName("t");
        task.setQueryConfig("{\"dataSetId\":8,\"sql\":\"select 1\"}");
        task.setOutputFormat("CSV");
        when(mapper.insert(task)).thenReturn(1);
        when(mapper.selectById(7L)).thenReturn(task);

        service.submitExportTask(task);

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Captor 会持有同一 DO 引用，后续 mutate 会让历史捕获“看起来全是最终态”，故用 argThat 分步校验
        verify(mapper).updateById(ArgumentMatchers
                .<ExportTaskDO>argThat(t -> ExportTaskStatus.RUNNING.name().equals(t.getStatus())));
        verify(mapper).updateById(ArgumentMatchers
                .<ExportTaskDO>argThat(t -> ExportTaskStatus.FAILED.name().equals(t.getStatus())
                        && t.getErrorMessage() != null
                        && t.getErrorMessage().contains("模拟数据源不可用")));
    }

    @Test
    void submitExportTaskShouldTransitionPendingRunningSuccessWhenQuerySucceeds(
            @TempDir Path exportDir) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        SemanticLayerService semanticLayerService = mock(SemanticLayerService.class);
        SemanticQueryResp resp = new SemanticQueryResp();
        resp.setColumns(List.of(new QueryColumn("列1", "STRING", "c1")));
        resp.setResultList(Collections.emptyList());
        when(semanticLayerService.queryByReq(any(), any())).thenReturn(resp);

        ExportTaskMapper mapper = mock(ExportTaskMapper.class);
        ExportTaskServiceImpl service = new ExportTaskServiceImpl(executor, semanticLayerService,
                mock(RowCountEstimator.class), mock(UserService.class), mock(DataSetService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "exportDir", exportDir.toString());

        ExportTaskDO task = new ExportTaskDO();
        task.setId(11L);
        task.setUserId(2L);
        task.setTenantId(3L);
        task.setDatasetId(8L);
        task.setTaskName("ok");
        task.setQueryConfig("{\"dataSetId\":8,\"sql\":\"select 1\"}");
        task.setOutputFormat("CSV");
        when(mapper.insert(task)).thenReturn(1);
        when(mapper.selectById(11L)).thenReturn(task);

        service.submitExportTask(task);
        assertEquals(ExportTaskStatus.PENDING.name(), task.getStatus());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        verify(mapper).updateById(ArgumentMatchers
                .<ExportTaskDO>argThat(t -> ExportTaskStatus.RUNNING.name().equals(t.getStatus())));
        verify(mapper).updateById(ArgumentMatchers
                .<ExportTaskDO>argThat(t -> ExportTaskStatus.SUCCESS.name().equals(t.getStatus())
                        && t.getRowCount() != null && t.getRowCount() == 0L
                        && t.getFileLocation() != null
                        && java.nio.file.Files.exists(java.nio.file.Path.of(t.getFileLocation()))));
    }
}
