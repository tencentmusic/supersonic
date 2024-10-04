package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏项表 Mapper 接口
 *
 * @author yannsu
 * @since 2023-11-09 03:49:33
 */
@Mapper
public interface CollectMapper extends BaseMapper<CollectDO> {
}
