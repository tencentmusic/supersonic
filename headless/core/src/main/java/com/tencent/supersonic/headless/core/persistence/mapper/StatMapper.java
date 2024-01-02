package com.tencent.supersonic.headless.core.persistence.mapper;


import com.tencent.supersonic.headless.common.server.pojo.QueryStat;
import com.tencent.supersonic.headless.common.core.request.ItemUseReq;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatMapper {

    Boolean createRecord(QueryStat queryStatInfo);

    List<QueryStat> getStatInfo(ItemUseReq itemUseCommend);
}
