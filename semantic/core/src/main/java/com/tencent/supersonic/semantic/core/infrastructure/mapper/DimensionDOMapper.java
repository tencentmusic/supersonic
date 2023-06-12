package com.tencent.supersonic.semantic.core.infrastructure.mapper;


import com.tencent.supersonic.semantic.core.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DimensionDOExample;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DimensionDOMapper {

    /**
     * @mbg.generated
     */
    long countByExample(DimensionDOExample example);

    /**
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int insert(DimensionDO record);

    /**
     * @mbg.generated
     */
    int insertSelective(DimensionDO record);

    /**
     * @mbg.generated
     */
    List<DimensionDO> selectByExampleWithBLOBs(DimensionDOExample example);

    /**
     * @mbg.generated
     */
    List<DimensionDO> selectByExample(DimensionDOExample example);

    /**
     * @mbg.generated
     */
    DimensionDO selectByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DimensionDO record);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(DimensionDO record);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKey(DimensionDO record);
}
