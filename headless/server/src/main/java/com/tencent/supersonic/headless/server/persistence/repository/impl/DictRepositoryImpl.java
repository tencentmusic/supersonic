package com.tencent.supersonic.headless.server.persistence.repository.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DictConfMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.DictTaskMapper;
import com.tencent.supersonic.headless.server.persistence.repository.DictRepository;
import com.tencent.supersonic.headless.server.utils.DictUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
public class DictRepositoryImpl implements DictRepository {

    private final DictTaskMapper dictTaskMapper;
    private final DictConfMapper dictConfMapper;
    private final DictUtils dictConverter;

    public DictRepositoryImpl(DictTaskMapper dictTaskMapper,
                              DictConfMapper dictConfMapper,
                              DictUtils dictConverter) {
        this.dictTaskMapper = dictTaskMapper;
        this.dictConfMapper = dictConfMapper;
        this.dictConverter = dictConverter;
    }

    @Override
    public Long addDictTask(DictTaskDO dictTaskDO) {
        dictTaskMapper.insert(dictTaskDO);
        return dictTaskDO.getId();
    }

    @Override
    public DictTaskResp queryLatestDictTask(DictSingleTaskReq taskReq) {
        DictTaskResp taskResp = new DictTaskResp();
        QueryWrapper<DictTaskDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DictTaskDO::getItemId, taskReq.getItemId());
        wrapper.lambda().eq(DictTaskDO::getType, taskReq.getType());
        List<DictTaskDO> dictTaskDOList = dictTaskMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dictTaskDOList)) {
            return taskResp;
        }
        taskResp = dictConverter.taskDO2Resp(dictTaskDOList.get(0));
        return taskResp;
    }

    @Override
    public Long addDictConf(DictConfDO dictConfDO) {
        dictConfMapper.insert(dictConfDO);
        return dictConfDO.getId();
    }

    @Override
    public Long editDictConf(DictConfDO dictConfDO) {
        dictConfMapper.updateById(dictConfDO);
        return dictConfDO.getId();
    }

    @Override
    public List<DictItemResp> queryDictConf(DictItemFilter dictItemFilter) {
        QueryWrapper<DictConfDO> wrapper = new QueryWrapper<>();
        if (Objects.nonNull(dictItemFilter.getId())) {
            wrapper.lambda().eq(DictConfDO::getId, dictItemFilter.getId());
        }
        if (Objects.nonNull(dictItemFilter.getType())) {
            wrapper.lambda().eq(DictConfDO::getType, dictItemFilter.getType().name());
        }
        if (Objects.nonNull(dictItemFilter.getItemId())) {
            wrapper.lambda().eq(DictConfDO::getItemId, dictItemFilter.getItemId());
        }
        if (Objects.nonNull(dictItemFilter.getStatus())) {
            wrapper.lambda().eq(DictConfDO::getStatus, dictItemFilter.getStatus().name());
        }
        List<DictConfDO> dictConfDOList = dictConfMapper.selectList(wrapper);
        return dictConverter.dictDOList2Req(dictConfDOList);
    }
}