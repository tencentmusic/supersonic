package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.tencent.supersonic.chat.api.pojo.request.DictTaskFilterReq;
import com.tencent.supersonic.chat.core.knowledge.DictConfig;
import com.tencent.supersonic.chat.core.knowledge.DimValueDictInfo;
import com.tencent.supersonic.chat.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.chat.server.persistence.mapper.DictConfMapper;
import com.tencent.supersonic.chat.server.persistence.mapper.DictTaskMapper;
import com.tencent.supersonic.chat.server.persistence.repository.DictRepository;
import com.tencent.supersonic.chat.server.util.DictTaskConverter;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;


@Repository
@Slf4j
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
        dictTaskMapper.updateTaskStatus(dictTaskDO);
        return true;
    }

    @Override
    public List<DimValueDictInfo> searchDictTaskList(DictTaskFilterReq filter) {
        List<DimValueDictInfo> dimValueDictDescList = new ArrayList<>();
        log.info("filter:{}", filter);
        List<DictTaskDO> dictTaskDOList = dictTaskMapper.searchDictTaskList(filter);
        if (!CollectionUtils.isEmpty(dictTaskDOList)) {
            dictTaskDOList.stream().forEach(dictTaskDO -> {
                DimValueDictInfo dimValueDictDesc = new DimValueDictInfo();
                BeanUtils.copyProperties(dictTaskDO, dimValueDictDesc);
                dimValueDictDesc.setStatus(TaskStatusEnum.of(dictTaskDO.getStatus()));
                if (StringUtils.isNotEmpty(dictTaskDO.getDimIds())) {
                    Set<Long> dimIds = JsonUtil.toSet(dictTaskDO.getDimIds(), Long.class);
                    dimValueDictDesc.setDimIds(dimIds);
                }
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
