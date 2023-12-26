package com.tencent.supersonic.headless.materialization.infrastructure.repository;

import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationConfFilter;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.headless.materialization.infrastructure.mapper.MaterializationElementDOMapper;
import com.tencent.supersonic.headless.materialization.domain.dataobject.MaterializationElementDOExample;
import com.tencent.supersonic.headless.materialization.domain.dataobject.MaterializationElementDOKey;
import com.tencent.supersonic.headless.materialization.domain.dataobject.MaterializationElementDOWithBLOBs;
import com.tencent.supersonic.headless.materialization.domain.pojo.MaterializationElement;
import com.tencent.supersonic.headless.materialization.domain.repository.MaterializationElementRepository;
import com.tencent.supersonic.headless.materialization.domain.utils.MaterializationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MaterializationElementRepositoryImpl implements MaterializationElementRepository {

    private final MaterializationElementDOMapper materializationElementDOMapper;

    public MaterializationElementRepositoryImpl(MaterializationElementDOMapper materializationElementDOMapper) {
        this.materializationElementDOMapper = materializationElementDOMapper;
    }

    @Override
    public Boolean insert(MaterializationElement materializationElement) {
        MaterializationElementDOWithBLOBs materializationElementDOWithBLOBs = MaterializationConverter
                .materialization2DO(materializationElement);
        materializationElementDOMapper.insert(materializationElementDOWithBLOBs);
        return true;
    }

    @Override
    public Boolean update(MaterializationElement materializationElement) {
        MaterializationElementDOKey key = new MaterializationElementDOKey();
        key.setId(materializationElement.getId());
        if (Objects.nonNull(materializationElement.getType())) {
            key.setType(materializationElement.getType().getName());
        }
        if (Objects.nonNull(materializationElement.getMaterializationId())) {
            key.setMaterializationId(materializationElement.getMaterializationId());
        }
        MaterializationElementDOWithBLOBs materializationElementDO = materializationElementDOMapper
                .selectByPrimaryKey(key);
        MaterializationConverter.convert(materializationElementDO, materializationElement);
        materializationElementDOMapper.updateByPrimaryKeyWithBLOBs(MaterializationConverter
                .convert(materializationElementDO, materializationElement));
        return true;
    }

    @Override
    public List<MaterializationElementResp> getMaterializationElementResp(MaterializationConfFilter filter) {
        List<MaterializationElementResp> materializationElementRespList = new ArrayList<>();
        MaterializationElementDOExample example = new MaterializationElementDOExample();
        MaterializationElementDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(filter.getType())) {
            criteria.andTypeEqualTo(filter.getType().getName());
        }
        if (Objects.nonNull(filter.getMaterializationId())) {
            criteria.andMaterializationIdEqualTo(filter.getMaterializationId());
        }
        if (Objects.nonNull(filter.getElementId())) {
            criteria.andIdEqualTo(filter.getElementId());
        }
        List<MaterializationElementDOWithBLOBs> materializationElementDOs = materializationElementDOMapper
                .selectByExampleWithBLOBs(example);
        if (!CollectionUtils.isEmpty(materializationElementDOs)) {
            materializationElementDOs.stream().forEach(materializationElementDO -> {
                materializationElementRespList.add(MaterializationConverter.elementDO2Resp(materializationElementDO));
            });
        }
        return materializationElementRespList;
    }

    @Override
    public Boolean cleanMaterializationElement(Long materializationId) {
        return materializationElementDOMapper.cleanMaterializationElement(materializationId);
    }
}