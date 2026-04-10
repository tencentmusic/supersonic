package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.service.impl.ExportTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
