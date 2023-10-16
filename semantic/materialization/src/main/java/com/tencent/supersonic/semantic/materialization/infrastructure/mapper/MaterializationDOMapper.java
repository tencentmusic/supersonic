package com.tencent.supersonic.semantic.materialization.infrastructure.mapper;

import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationDO;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationDOExample;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationDOWithBLOBs;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MaterializationDOMapper {
    long countByExample(MaterializationDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(MaterializationDOWithBLOBs record);

    int insertSelective(MaterializationDOWithBLOBs record);

    List<MaterializationDOWithBLOBs> selectByExampleWithBLOBs(MaterializationDOExample example);

    List<MaterializationDO> selectByExample(MaterializationDOExample example);

    MaterializationDOWithBLOBs selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(MaterializationDOWithBLOBs record);

    int updateByPrimaryKeyWithBLOBs(MaterializationDOWithBLOBs record);

    int updateByPrimaryKey(MaterializationDO record);
}