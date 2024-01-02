package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.AppDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppMapper extends BaseMapper<AppDO> {

}
