package com.tencent.supersonic.headless.query.persistence.repository;

import com.tencent.supersonic.headless.common.model.pojo.QueryStat;
import com.tencent.supersonic.headless.common.query.request.ItemUseReq;
import com.tencent.supersonic.headless.common.query.response.ItemUseResp;
import java.util.List;

public interface StatRepository {

    Boolean createRecord(QueryStat queryStatInfo);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend);

    List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend);
}