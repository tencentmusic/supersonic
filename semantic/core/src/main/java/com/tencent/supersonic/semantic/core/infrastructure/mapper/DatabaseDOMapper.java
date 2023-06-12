package com.tencent.supersonic.semantic.core.infrastructure.mapper;

import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DatabaseDOMapper {

    /**
     * @mbg.generated
     */
    long countByExample(DatabaseDOExample example);

    /**
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int insert(DatabaseDO record);

    /**
     * @mbg.generated
     */
    int insertSelective(DatabaseDO record);

    /**
     * @mbg.generated
     */
    List<DatabaseDO> selectByExampleWithBLOBs(DatabaseDOExample example);

    /**
     * @mbg.generated
     */
    List<DatabaseDO> selectByExample(DatabaseDOExample example);

    /**
     * @mbg.generated
     */
    DatabaseDO selectByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DatabaseDO record);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(DatabaseDO record);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKey(DatabaseDO record);
}