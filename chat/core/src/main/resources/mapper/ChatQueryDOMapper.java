package com.tencent.supersonic.domain.chat.infrastructure.persistence.mybatis.mapper;

import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDOExample;
import java.util.List;

public interface ChatQueryDOMapper {
    /**
     *
     * @mbg.generated
     */
    long countByExample(ChatQueryDOExample example);

    /**
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int insert(ChatQueryDO record);

    /**
     *
     * @mbg.generated
     */
    int insertSelective(ChatQueryDO record);

    /**
     *
     * @mbg.generated
     */
    List<ChatQueryDO> selectByExampleWithBLOBs(ChatQueryDOExample example);

    /**
     *
     * @mbg.generated
     */
    List<ChatQueryDO> selectByExample(ChatQueryDOExample example);

    /**
     *
     * @mbg.generated
     */
    ChatQueryDO selectByPrimaryKey(Long id);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(ChatQueryDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKeyWithBLOBs(ChatQueryDO record);

    /**
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(ChatQueryDO record);
}