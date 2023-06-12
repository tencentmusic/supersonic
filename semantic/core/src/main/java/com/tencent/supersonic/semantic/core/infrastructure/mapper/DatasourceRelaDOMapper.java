package com.tencent.supersonic.semantic.core.infrastructure.mapper;

import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceRelaDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceRelaDOExample;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DatasourceRelaDOMapper {

    /**
     * @mbg.generated
     */
    long countByExample(DatasourceRelaDOExample example);

    /**
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int insert(DatasourceRelaDO record);

    /**
     * @mbg.generated
     */
    int insertSelective(DatasourceRelaDO record);

    /**
     * @mbg.generated
     */
    List<DatasourceRelaDO> selectByExample(DatasourceRelaDOExample example);

    /**
     * @mbg.generated
     */
    DatasourceRelaDO selectByPrimaryKey(Long id);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DatasourceRelaDO record);

    /**
     * @mbg.generated
     */
    int updateByPrimaryKey(DatasourceRelaDO record);
}