package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.DomainDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DomainDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DomainDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(DomainDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(DomainDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(DomainDO record);

    /**
     *
     * @mbg.generated
     */
    List<DomainDO> selectByExample(DomainDOExample example);

    /**
     *
     * @mbg.generated
     */
    DomainDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DomainDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(DomainDO record);
}