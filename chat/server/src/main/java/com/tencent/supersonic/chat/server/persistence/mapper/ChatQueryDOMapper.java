package com.tencent.supersonic.chat.server.persistence.mapper;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDOExample;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatQueryDOMapper {

    int insert(ChatQueryDO record);

    List<ChatQueryDO> selectByExampleWithBLOBs(ChatQueryDOExample example);

    int updateByPrimaryKeyWithBLOBs(ChatQueryDO record);

    Boolean deleteByPrimaryKey(Long questionId);

    ChatQueryDO selectByPrimaryKey(Long questionId);
}
