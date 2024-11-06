package com.tencent.supersonic.common.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.common.persistence.dataobject.ChatModelDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatModelMapper extends BaseMapper<ChatModelDO> {
}
