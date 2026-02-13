package com.tencent.supersonic.feishu.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeishuQuerySessionMapper extends BaseMapper<FeishuQuerySessionDO> {
}
