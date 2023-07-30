package com.tencent.supersonic.semantic.query.persistence.repository;

import com.tencent.supersonic.semantic.api.model.pojo.QueryStat;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import java.util.List;

public interface StatRepository {

    Boolean createRecord(QueryStat queryStatInfo);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend);

    List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend);
}