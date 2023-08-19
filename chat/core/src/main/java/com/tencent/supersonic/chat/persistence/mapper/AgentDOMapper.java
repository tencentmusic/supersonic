package com.tencent.supersonic.chat.persistence.mapper;

import com.tencent.supersonic.chat.persistence.dataobject.AgentDO;
import com.tencent.supersonic.chat.persistence.dataobject.AgentDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(AgentDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     *
     * @mbg.generated
     */
    int insert(AgentDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(AgentDO record);

    /**
     *
     * @mbg.generated
     */
    List<AgentDO> selectByExample(AgentDOExample example);

    /**
     *
     * @mbg.generated
     */
    AgentDO selectByPrimaryKey(Integer id);

    /**
     *
     * @mbg.generated
     */
    int updateByExampleSelective(@Param("record") AgentDO record, @Param("example") AgentDOExample example);

    /**
     *
     * @mbg.generated
     */
    int updateByExample(@Param("record") AgentDO record, @Param("example") AgentDOExample example);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(AgentDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(AgentDO record);
}