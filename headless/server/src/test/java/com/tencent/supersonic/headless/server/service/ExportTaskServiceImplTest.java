package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.service.impl.ExportTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ExportTaskServiceImplTest {

    @Test
    void submitExportTaskShouldRejectBlankQueryConfig() {
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        ExportTaskMapper mapper = mock(ExportTaskMapper.class);
        ExportTaskServiceImpl service = new ExportTaskServiceImpl(executor, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        ExportTaskDO task = new ExportTaskDO();
        task.setDatasetId(1L);
        task.setOutputFormat("EXCEL");

        assertThrows(IllegalArgumentException.class, () -> service.submitExportTask(task));
    }
}
