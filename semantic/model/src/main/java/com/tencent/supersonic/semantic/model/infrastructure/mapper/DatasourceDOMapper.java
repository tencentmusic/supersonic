package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DatasourceDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(DatasourceDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(DatasourceDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(DatasourceDO record);

    /**
     *
     * @mbg.generated
     */
    List<DatasourceDO> selectByExampleWithBLOBs(DatasourceDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<DatasourceDO> selectByExample(DatasourceDOExample example);

    /**
     *
     * @mbg.generated
     */
    DatasourceDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DatasourceDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(DatasourceDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(DatasourceDO record);
}