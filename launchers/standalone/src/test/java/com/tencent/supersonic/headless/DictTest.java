package com.tencent.supersonic.headless;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.ItemValueConfig;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DictConfMapper;
import com.tencent.supersonic.headless.server.web.service.DictTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class DictTest extends BaseTest {
    @Autowired
    private DictConfMapper confMapper;

    @Autowired
    private DictTaskService taskService;

    @Test
    public void insertConf() {
        DictConfDO confDO = new DictConfDO();
        Date createAt = new Date();
        confDO.setType(TypeEnums.DIMENSION.name());
        confDO.setItemId(1L);
        confDO.setConfig(JsonUtil.toString(new ItemValueConfig()));
        confDO.setStatus(StatusEnum.ONLINE.getStatus());
        confDO.setCreatedAt(createAt);
        confDO.setCreatedBy("admin");
        confMapper.insert(confDO);
        DictConfDO confDODb = confMapper.selectById(1L);
        System.out.println(confDODb.getId());
    }

    @Test
    public void editConf() {
        DictConfDO confDO = new DictConfDO();
        Date createAt = new Date();
        confDO.setType(TypeEnums.DIMENSION.name());
        confDO.setItemId(3L);
        ItemValueConfig config = new ItemValueConfig();
        config.setMetricId(4L);
        config.setBlackList(new ArrayList<>(Arrays.asList("1", "3")));
        config.setWhiteList(new ArrayList<>(Arrays.asList("2", "4")));
        confDO.setConfig(JsonUtil.toString(config));
        confDO.setStatus(TaskStatusEnum.PENDING.getStatus());
        confDO.setCreatedAt(createAt);
        confDO.setCreatedBy("admin");
        confMapper.insert(confDO);
        DictConfDO confDODb = confMapper.selectById(1L);

        confDO.setStatus(StatusEnum.OFFLINE.getStatus());
        // config.setMetricId(3L);
        config.setBlackList(new ArrayList<>(Arrays.asList("p2")));
        config.setWhiteList(new ArrayList<>(Arrays.asList("p10", "p12")));
        confDODb.setConfig(JsonUtil.toString(config));
        confMapper.updateById(confDODb);
        DictConfDO confDODb1 = confMapper.selectById(1L);
        System.out.println(confDODb1.getId());
    }

    @Test
    void testBatchInsert() {
        for (int i = 0; i < 5; i++) {
            insertConf();
        }
        QueryWrapper<DictConfDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DictConfDO::getType, "DIMENSION");
        List<DictConfDO> dictConfDOList = confMapper.selectList(wrapper);
        System.out.println(dictConfDOList);
    }

    @Test
    void testAddTask() {
        editConf();
        DictConfDO confDODb = confMapper.selectById(1L);
        DictSingleTaskReq dictTask = DictSingleTaskReq.builder().itemId(confDODb.getItemId())
                .type(TypeEnums.DIMENSION).build();
        taskService.addDictTask(dictTask, null);
        DictSingleTaskReq taskReq = DictSingleTaskReq.builder().itemId(3L).type(TypeEnums.DIMENSION).build();
        taskService.deleteDictTask(taskReq, null);
        System.out.println();
    }

}