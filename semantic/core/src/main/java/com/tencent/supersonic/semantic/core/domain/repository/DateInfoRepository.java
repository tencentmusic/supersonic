package com.tencent.supersonic.semantic.core.domain.repository;


import com.tencent.supersonic.semantic.api.core.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.core.request.DateInfoReq;
import com.tencent.supersonic.semantic.core.domain.dataobject.DateInfoDO;

import java.util.List;

public interface DateInfoRepository {

    Integer upsertDateInfo(List<DateInfoReq> dateInfoReqs);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);

}
