package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DimensionDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(DimensionDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(DimensionDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(DimensionDO record);

    /**
     *
     * @mbg.generated
     */
    List<DimensionDO> selectByExampleWithBLOBs(DimensionDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<DimensionDO> selectByExample(DimensionDOExample example);

    /**
     *
     * @mbg.generated
     */
    DimensionDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DimensionDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(DimensionDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(DimensionDO record);
}