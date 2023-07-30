package com.tencent.supersonic.semantic.query.persistence.mapper;


import com.tencent.supersonic.semantic.api.model.pojo.QueryStat;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatMapper {

    Boolean createRecord(QueryStat queryStatInfo);

    List<QueryStat> getStatInfo(ItemUseReq itemUseCommend);
}
