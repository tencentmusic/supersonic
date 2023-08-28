package com.tencent.supersonic.chat.persistence.mapper;

import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatQueryDOMapper {

    int insert(ChatQueryDO record);

    List<ChatQueryDO> selectByExampleWithBLOBs(ChatQueryDOExample example);

    int updateByPrimaryKeyWithBLOBs(ChatQueryDO record);

}
