package com.tencent.supersonic.headless.server.persistence.repository.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DictConfMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.DictTaskMapper;
import com.tencent.supersonic.headless.server.persistence.repository.DictRepository;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.utils.DictUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class DictRepositoryImpl implements DictRepository {

    @Value("${s2.dict.task.num:10}")
    private Integer dictTaskNum;

    private final DictTaskMapper dictTaskMapper;
    private final DictConfMapper dictConfMapper;
    private final DictUtils dictConverter;
    private final DimensionService dimensionService;

    public DictRepositoryImpl(DictTaskMapper dictTaskMapper, DictConfMapper dictConfMapper,
                              DictUtils dictConverter, DimensionService dimensionService) {
        this.dictTaskMapper = dictTaskMapper;
        this.dictConfMapper = dictConfMapper;
        this.dictConverter = dictConverter;
        this.dimensionService = dimensionService;
    }

    @Override
    public Long addDictTask(DictTaskDO dictTaskDO) {
        dictTaskMapper.insert(dictTaskDO);
        return dictTaskDO.getId();
    }

    @Override
    public Long editDictTask(DictTaskDO dictTaskDO) {
        dictTaskMapper.updateById(dictTaskDO);
        return dictTaskDO.getId();
    }

    @Override
    public DictTaskDO queryDictTask(DictItemFilter filter) {
        QueryWrapper<DictTaskDO> wrapper = new QueryWrapper<>();
        if (Objects.nonNull(filter.getItemId())) {
            wrapper.lambda().eq(DictTaskDO::getItemId, filter.getItemId());
        }
        if (Objects.nonNull(filter.getType())) {
            wrapper.lambda().eq(DictTaskDO::getType, filter.getType());
        }
        if (Objects.nonNull(filter.getId())) {
            wrapper.lambda().eq(DictTaskDO::getId, filter.getId());
        }

        List<DictTaskDO> dictTaskDOList = dictTaskMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dictTaskDOList)) {
            return null;
        }
        return dictTaskDOList.get(0);
    }

    @Override
    public DictTaskDO queryDictTaskById(Long id) {
        return dictTaskMapper.selectById(id);
    }

    @Override
    public DictTaskResp queryLatestDictTask(DictSingleTaskReq taskReq) {
        DictTaskResp taskResp = new DictTaskResp();
        QueryWrapper<DictTaskDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DictTaskDO::getItemId, taskReq.getItemId());
        wrapper.lambda().eq(DictTaskDO::getType, taskReq.getType());
        List<DictTaskDO> dictTaskDOList = dictTaskMapper.selectList(wrapper).stream()
                .sorted(Comparator.comparing(DictTaskDO::getCreatedAt).reversed())
                .limit(dictTaskNum).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dictTaskDOList)) {
            return taskResp;
        }
        taskResp = dictConverter.taskDO2Resp(dictTaskDOList.get(0));
        DimensionResp dimension = dimensionService.getDimension(taskReq.getItemId());
        taskResp.setBizName(dimension.getBizName());
        taskResp.setModelId(dimension.getModelId());
        return taskResp;
    }

    @Override
    public Long addDictConf(DictConfDO dictConfDO) {
        dictConfMapper.insert(dictConfDO);
        return dictConfDO.getId();
    }

    @Override
    public Long editDictConf(DictConfDO dictConfDO) {
        DictItemFilter filter = DictItemFilter.builder()
                .type(TypeEnums.valueOf(dictConfDO.getType()))
                .itemId(dictConfDO.getItemId())
                .build();

        List<DictConfDO> dictConfDOList = getDictConfDOList(filter);
        if (CollectionUtils.isEmpty(dictConfDOList)) {
            return -1L;
        }
        dictConfDO.setId(dictConfDOList.get(0).getId());
        dictConfMapper.updateById(dictConfDO);
        return dictConfDO.getId();
    }

    @Override
    public List<DictItemResp> queryDictConf(DictItemFilter dictItemFilter) {
        List<DictConfDO> dictConfDOList = getDictConfDOList(dictItemFilter);
        return dictConverter.dictDOList2Req(dictConfDOList);
    }

    private List<DictConfDO> getDictConfDOList(DictItemFilter dictItemFilter) {
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
        return dictConfDOList;
    }
}