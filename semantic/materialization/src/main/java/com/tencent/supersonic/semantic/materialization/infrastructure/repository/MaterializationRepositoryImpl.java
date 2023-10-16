package com.tencent.supersonic.semantic.materialization.infrastructure.repository;

import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationDOWithBLOBs;
import com.tencent.supersonic.semantic.materialization.domain.pojo.Materialization;
import com.tencent.supersonic.semantic.materialization.domain.repository.MaterializationRepository;
import com.tencent.supersonic.semantic.materialization.domain.utils.MaterializationConverter;
import com.tencent.supersonic.semantic.materialization.infrastructure.mapper.MaterializationDOCustomMapper;
import com.tencent.supersonic.semantic.materialization.infrastructure.mapper.MaterializationDOMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class MaterializationRepositoryImpl implements MaterializationRepository {

    private final MaterializationDOMapper materializationDOMapper;
    private final MaterializationDOCustomMapper materializationDOCustomMapper;

    public MaterializationRepositoryImpl(MaterializationDOMapper materializationDOMapper,
                                         MaterializationDOCustomMapper materializationDOCustomMapper) {
        this.materializationDOMapper = materializationDOMapper;
        this.materializationDOCustomMapper = materializationDOCustomMapper;
    }

    @Override
    public Boolean insert(Materialization materialization) {
        MaterializationDOWithBLOBs materializationDOWithBLOBs = MaterializationConverter
                .materialization2DO(materialization);
        materializationDOMapper.insert(materializationDOWithBLOBs);
        return true;
    }

    @Override
    public Boolean update(Materialization materialization) {
        MaterializationDOWithBLOBs materializationDOWithBLOBs = materializationDOMapper
                .selectByPrimaryKey(materialization.getId());
        materializationDOMapper.updateByPrimaryKeyWithBLOBs(MaterializationConverter
                .convert(materializationDOWithBLOBs, materialization));
        return true;
    }

    @Override
    public List<MaterializationResp> getMaterializationResp(MaterializationFilter filter) {
        List<MaterializationResp> materializationRespList = new ArrayList<>();
        List<MaterializationDOWithBLOBs> materializationDOWithBLOBsList = materializationDOCustomMapper
                .getMaterializationResp(filter);
        if (!CollectionUtils.isEmpty(materializationDOWithBLOBsList)) {
            materializationDOWithBLOBsList.stream().forEach(materializationDO -> {
                materializationRespList.add(MaterializationConverter.convert2Resp(materializationDO));
            });
        }
        return materializationRespList;
    }

    @Override
    public MaterializationResp getMaterialization(Long id) {
        return MaterializationConverter.convert2Resp(materializationDOMapper.selectByPrimaryKey(id));
    }
}