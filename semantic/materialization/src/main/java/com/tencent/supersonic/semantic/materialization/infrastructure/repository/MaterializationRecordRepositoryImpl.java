package com.tencent.supersonic.semantic.materialization.infrastructure.repository;


import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationRecordFilter;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationRecordResp;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationRecordDO;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationRecordDOExample;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationRecord;
import com.tencent.supersonic.semantic.materialization.domain.repository.MaterializationRecordRepository;
import com.tencent.supersonic.semantic.materialization.domain.utils.MaterializationRecordConverter;
import com.tencent.supersonic.semantic.materialization.infrastructure.mapper.MaterializationRecordDOMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class MaterializationRecordRepositoryImpl implements MaterializationRecordRepository {

    private final MaterializationRecordDOMapper mapper;

    public MaterializationRecordRepositoryImpl(MaterializationRecordDOMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Boolean insertMaterializationRecord(MaterializationRecord materializationRecord) {
        MaterializationRecordDO materializationRecordDO = MaterializationRecordConverter
                .materializationRecord2DO(materializationRecord);
        mapper.insert(materializationRecordDO);
        return true;
    }

    @Override
    public Boolean updateMaterializationRecord(MaterializationRecord materializationRecord) {
        MaterializationRecordDO materializationRecordDO = mapper.selectByPrimaryKey(materializationRecord.getId());
        if (Objects.nonNull(materializationRecordDO)) {
            MaterializationRecordConverter.convert(materializationRecordDO, materializationRecord);
        } else {
            materializationRecordDO = MaterializationRecordConverter.materializationRecord2DO(materializationRecord);
        }

        if (Objects.isNull(materializationRecord.getId())) {
            mapper.updateByBizName(materializationRecordDO);
        } else {
            mapper.updateByPrimaryKey(materializationRecordDO);
        }
        return true;
    }

    @Override
    public List<MaterializationRecordResp> getMaterializationRecordList(MaterializationRecordFilter filter) {
        List<MaterializationRecordResp> materializationRecordRespList = new ArrayList<>();
        MaterializationRecordDOExample example = getExample(filter);

        List<MaterializationRecordDO> materializationRecordDOS = mapper.selectByExampleWithBLOBs(example);
        if (!CollectionUtils.isEmpty(materializationRecordDOS)) {
            materializationRecordDOS.stream().forEach(recordDO -> materializationRecordRespList.add(
                    MaterializationRecordConverter.materializationRecordDO2Resp(recordDO)));
        }
        return materializationRecordRespList;
    }

    @Override
    public long getCount(MaterializationRecordFilter filter) {
        MaterializationRecordDOExample example = getExample(filter);
        return mapper.countByExample(example);
    }

    private MaterializationRecordDOExample getExample(MaterializationRecordFilter filter) {
        MaterializationRecordDOExample example = new MaterializationRecordDOExample();
        MaterializationRecordDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(filter.getId())) {
            criteria.andIdEqualTo(filter.getId());
        }
        if (Objects.nonNull(filter.getMaterializationId())) {
            criteria.andMaterializationIdEqualTo(filter.getMaterializationId());
        }
        if (!CollectionUtils.isEmpty(filter.getMaterializationIds())) {
            criteria.andMaterializationIdIn(filter.getMaterializationIds());
        }
        if (Objects.nonNull(filter.getElementType())) {
            criteria.andElementTypeEqualTo(filter.getElementType().getName());
        }
        if (Objects.nonNull(filter.getElementId())) {
            criteria.andElementIdEqualTo(filter.getElementId());
        }
        if (Objects.nonNull(filter.getElementName())) {
            criteria.andElementNameEqualTo(filter.getElementName());
        }
        if (Objects.nonNull(filter.getTaskStatus())) {
            criteria.andStateIn(filter.getTaskStatus().stream().map(s -> s.getStatus()).collect(Collectors.toList()));
        }
        if (Objects.nonNull(filter.getTaskId())) {
            criteria.andTaskIdEqualTo(filter.getTaskId());
        }
        if (Objects.nonNull(filter.getCreatedBy())) {
            criteria.andCreatedByEqualTo(filter.getCreatedBy());
        }
        if (Objects.nonNull(filter.getCreatedAt())) {
            criteria.andCreatedAtGreaterThanOrEqualTo(filter.getCreatedAt());
        }
        if (Objects.nonNull(filter.getStartDataTime())) {
            criteria.andDataTimeGreaterThanOrEqualTo(filter.getStartDataTime());
        }
        if (Objects.nonNull(filter.getEndDataTime())) {
            criteria.andDataTimeLessThanOrEqualTo(filter.getEndDataTime());
        }
        return example;
    }
}