package com.tencent.supersonic.semantic.materialization.infrastructure.mapper;

import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationElementDO;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationElementDOExample;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationElementDOKey;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationElementDOWithBLOBs;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface MaterializationElementDOMapper {
    long countByExample(MaterializationElementDOExample example);

    int deleteByPrimaryKey(MaterializationElementDOKey key);

    int insert(MaterializationElementDOWithBLOBs record);

    int insertSelective(MaterializationElementDOWithBLOBs record);

    List<MaterializationElementDOWithBLOBs> selectByExampleWithBLOBs(MaterializationElementDOExample example);

    List<MaterializationElementDO> selectByExample(MaterializationElementDOExample example);

    MaterializationElementDOWithBLOBs selectByPrimaryKey(MaterializationElementDOKey key);

    int updateByPrimaryKeySelective(MaterializationElementDOWithBLOBs record);

    int updateByPrimaryKeyWithBLOBs(MaterializationElementDOWithBLOBs record);

    int updateByPrimaryKey(MaterializationElementDO record);

    Boolean cleanMaterializationElement(Long materializationId);
}