package com.tencent.supersonic.chat.server.persistence.mapper.custom;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowCaseCustomMapper {

    List<ChatQueryDO> queryShowCase(int start, int limit, int agentId, String userName);
}
