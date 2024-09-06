package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.api.pojo.QueryStat;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StatMapper {

    Boolean createRecord(QueryStat queryStatInfo);

    List<QueryStat> getStatInfo(ItemUseReq itemUseCommend);
}
