package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDOExample;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModelDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(ModelDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(ModelDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(ModelDO record);

    /**
     *
     * @mbg.generated
     */
    List<ModelDO> selectByExampleWithBLOBs(ModelDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<ModelDO> selectByExample(ModelDOExample example);

    /**
     *
     * @mbg.generated
     */
    ModelDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByExampleSelective(@Param("record") ModelDO record, @Param("example") ModelDOExample example);

    /**
     *
     * @mbg.generated
     */
    int updateByExampleWithBLOBs(@Param("record") ModelDO record, @Param("example") ModelDOExample example);

    /**
     *
     * @mbg.generated
     */
    int updateByExample(@Param("record") ModelDO record, @Param("example") ModelDOExample example);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(ModelDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(ModelDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(ModelDO record);
}