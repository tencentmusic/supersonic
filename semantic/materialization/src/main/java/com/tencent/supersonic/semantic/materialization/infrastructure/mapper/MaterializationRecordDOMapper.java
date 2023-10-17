package com.tencent.supersonic.semantic.materialization.infrastructure.mapper;

import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationRecordDO;
import com.tencent.supersonic.semantic.materialization.domain.dataobject.MaterializationRecordDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface MaterializationRecordDOMapper {
    long countByExample(MaterializationRecordDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(MaterializationRecordDO record);

    int insertSelective(MaterializationRecordDO record);

    List<MaterializationRecordDO> selectByExampleWithBLOBs(MaterializationRecordDOExample example);

    List<MaterializationRecordDO> selectByExample(MaterializationRecordDOExample example);

    MaterializationRecordDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(MaterializationRecordDO record);

    int updateByPrimaryKeyWithBLOBs(MaterializationRecordDO record);

    int updateByBizName(MaterializationRecordDO record);

    int updateByPrimaryKey(MaterializationRecordDO record);
}