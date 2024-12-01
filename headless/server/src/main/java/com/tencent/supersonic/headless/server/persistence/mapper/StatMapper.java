package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.api.pojo.QueryStat;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.server.persistence.dataobject.QueryStatDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StatMapper extends BaseMapper<QueryStatDO> {

    List<QueryStat> getStatInfo(ItemUseReq itemUseCommend);
}
