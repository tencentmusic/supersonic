package com.tencent.supersonic.headless.core.persistence.repository;

import com.tencent.supersonic.headless.api.pojo.QueryStat;
import com.tencent.supersonic.headless.api.request.ItemUseReq;
import com.tencent.supersonic.headless.api.response.ItemUseResp;
import java.util.List;

public interface StatRepository {

    Boolean createRecord(QueryStat queryStatInfo);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend);

    List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend);
}