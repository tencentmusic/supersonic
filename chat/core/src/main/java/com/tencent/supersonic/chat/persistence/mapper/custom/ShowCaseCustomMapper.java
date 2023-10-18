package com.tencent.supersonic.chat.persistence.mapper.custom;

import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ShowCaseCustomMapper {

    List<ChatQueryDO> queryShowCase(int start, int limit, int agentId, String userName);

}
