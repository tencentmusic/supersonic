package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemoryDO> {

}