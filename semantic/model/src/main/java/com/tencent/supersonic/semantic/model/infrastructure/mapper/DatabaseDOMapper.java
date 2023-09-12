package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatabaseDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DatabaseDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(DatabaseDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(DatabaseDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(DatabaseDO record);

    /**
     *
     * @mbg.generated
     */
    List<DatabaseDO> selectByExampleWithBLOBs(DatabaseDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<DatabaseDO> selectByExample(DatabaseDOExample example);

    /**
     *
     * @mbg.generated
     */
    DatabaseDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DatabaseDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(DatabaseDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(DatabaseDO record);
}