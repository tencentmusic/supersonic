package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConnectionEventMapper extends BaseMapper<ConnectionEventDO> {
}
