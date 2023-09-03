package com.tencent.supersonic.knowledge.persistence.repository;

import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.knowledge.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.knowledge.utils.DictTaskConverter;
import com.tencent.supersonic.knowledge.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.knowledge.dictionary.DictConfig;
import com.tencent.supersonic.knowledge.dictionary.DictTaskFilter;
import com.tencent.supersonic.knowledge.dictionary.DimValueDictInfo;
import com.tencent.supersonic.knowledge.persistence.mapper.DictConfMapper;
import com.tencent.supersonic.knowledge.persistence.mapper.DictTaskMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;


@Repository
public class DictRepositoryImpl implements DictRepository {

    private final DictTaskMapper dictTaskMapper;
    private final DictConfMapper dictConfMapper;

    public DictRepositoryImpl(DictTaskMapper dictTaskMapper,
                              DictConfMapper dictConfMapper) {
        this.dictTaskMapper = dictTaskMapper;
        this.dictConfMapper = dictConfMapper;
    }

    @Override
    public Long createDimValueDictTask(DictTaskDO dictTaskDO) {
        dictTaskMapper.createDimValueTask(dictTaskDO);
        return dictTaskDO.getId();
    }


    @Override
    public Boolean updateDictTaskStatus(Integer status, DictTaskDO dictTaskDO) {
        dictTaskDO.setStatus(status);
        Date createdAt = dictTaskDO.getCreatedAt();
        long elapsedMs = System.currentTimeMillis() - createdAt.getTime();
        dictTaskDO.setElapsedMs(elapsedMs);
        CompletableFuture.supplyAsync(() -> {
            dictTaskMapper.updateTaskStatus(dictTaskDO);
            return null;
        });
        return true;
    }

    @Override
    public List<DimValueDictInfo> searchDictTaskList(DictTaskFilter filter) {
        List<DimValueDictInfo> dimValueDictDescList = new ArrayList<>();
        List<DictTaskDO> dictTaskDOList = dictTaskMapper.searchDictTaskList(filter);
        if (!CollectionUtils.isEmpty(dictTaskDOList)) {
            dictTaskDOList.stream().forEach(dictTaskPO -> {
                DimValueDictInfo dimValueDictDesc = new DimValueDictInfo();
                BeanUtils.copyProperties(dictTaskPO, dimValueDictDesc);
                dimValueDictDesc.setStatus(TaskStatusEnum.of(dictTaskPO.getStatus()));
                dimValueDictDescList.add(dimValueDictDesc);
            });
        }
        return dimValueDictDescList;
    }

    @Override
    public DictConfig getDictInfoByModelId(Long modelId) {
        DictConfDO dictConfDO = dictConfMapper.getDictInfoByModelId(modelId);
        if (Objects.isNull(dictConfDO)) {
            return null;
        }
        return DictTaskConverter.dictConfPO2Config(dictConfDO);
    }
}
