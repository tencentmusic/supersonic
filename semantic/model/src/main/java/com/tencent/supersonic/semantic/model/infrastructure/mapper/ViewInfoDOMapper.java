package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.ViewInfoDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ViewInfoDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(ViewInfoDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(ViewInfoDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(ViewInfoDO record);

    /**
     *
     * @mbg.generated
     */
    List<ViewInfoDO> selectByExampleWithBLOBs(ViewInfoDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<ViewInfoDO> selectByExample(ViewInfoDOExample example);

    /**
     *
     * @mbg.generated
     */
    ViewInfoDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(ViewInfoDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(ViewInfoDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(ViewInfoDO record);
}