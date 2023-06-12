package com.tencent.supersonic.chat.infrastructure.mapper;

import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatQueryDOMapper {

    long countByExample(ChatQueryDOExample example);

    int deleteByPrimaryKey(Long questionId);


    int insert(ChatQueryDO record);

    int insertSelective(ChatQueryDO record);

    List<ChatQueryDO> selectByExampleWithBLOBs(ChatQueryDOExample example);

    List<ChatQueryDO> selectByExample(ChatQueryDOExample example);

    ChatQueryDO selectByPrimaryKey(Long questionId);

    int updateByPrimaryKeySelective(ChatQueryDO record);

    int updateByPrimaryKeyWithBLOBs(ChatQueryDO record);

    int updateByPrimaryKey(ChatQueryDO record);
}