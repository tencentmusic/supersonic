package com.tencent.supersonic.knowledge.infrastructure.repository;

import com.tencent.supersonic.common.enums.TaskStatusEnum;
import com.tencent.supersonic.knowledge.domain.converter.DictTaskConverter;
import com.tencent.supersonic.knowledge.domain.dataobject.DictConfPO;
import com.tencent.supersonic.knowledge.domain.dataobject.DimValueDictTaskPO;
import com.tencent.supersonic.knowledge.domain.pojo.DictConfig;
import com.tencent.supersonic.knowledge.domain.pojo.DictTaskFilter;
import com.tencent.supersonic.knowledge.domain.pojo.DimValueDictInfo;
import com.tencent.supersonic.knowledge.domain.repository.DictRepository;
import com.tencent.supersonic.knowledge.infrastructure.custom.DictConfMapper;
import com.tencent.supersonic.knowledge.infrastructure.custom.DictTaskMapper;
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
    public Long createDimValueDictTask(DimValueDictTaskPO dimValueDictTaskPO) {
        dictTaskMapper.createDimValueTask(dimValueDictTaskPO);
        return dimValueDictTaskPO.getId();
    }


    @Override
    public Boolean updateDictTaskStatus(Integer status, DimValueDictTaskPO dimValueDictTaskPO) {
        dimValueDictTaskPO.setStatus(status);
        Date createdAt = dimValueDictTaskPO.getCreatedAt();
        long elapsedMs = System.currentTimeMillis() - createdAt.getTime();
        dimValueDictTaskPO.setElapsedMs(elapsedMs);
        CompletableFuture.supplyAsync(() -> {
            dictTaskMapper.updateTaskStatus(dimValueDictTaskPO);
            return null;
        });
        return true;
    }

    @Override
    public List<DimValueDictInfo> searchDictTaskList(DictTaskFilter filter) {
        List<DimValueDictInfo> dimValueDictDescList = new ArrayList<>();
        List<DimValueDictTaskPO> dimValueDictTaskPOList = dictTaskMapper.searchDictTaskList(filter);
        if (!CollectionUtils.isEmpty(dimValueDictTaskPOList)) {
            dimValueDictTaskPOList.stream().forEach(dictTaskPO -> {
                DimValueDictInfo dimValueDictDesc = new DimValueDictInfo();
                BeanUtils.copyProperties(dictTaskPO, dimValueDictDesc);
                dimValueDictDesc.setStatus(TaskStatusEnum.of(dictTaskPO.getStatus()));
                dimValueDictDescList.add(dimValueDictDesc);
            });
        }
        return dimValueDictDescList;
    }

    @Override
    public Boolean createDictConf(DictConfPO dictConfPO) {
        return dictConfMapper.createDictConf(dictConfPO);
    }

    @Override
    public Boolean editDictConf(DictConfPO dictConfPO) {
        return dictConfMapper.editDictConf(dictConfPO);
    }

    @Override
    public Boolean upsertDictInfo(DictConfPO dictConfPO) {
        return dictConfMapper.upsertDictInfo(dictConfPO);
    }

    @Override
    public DictConfig getDictInfoByDomainId(Long domainId) {
        DictConfPO dictConfPO = dictConfMapper.getDictInfoByDomainId(domainId);
        if (Objects.isNull(dictConfPO)) {
            return null;
        }
        return DictTaskConverter.dictConfPO2Config(dictConfPO);
    }
}