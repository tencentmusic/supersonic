package com.tencent.supersonic.semantic.model.domain.repository;


import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.request.DateInfoReq;
import com.tencent.supersonic.semantic.model.domain.dataobject.DateInfoDO;

import java.util.List;

public interface DateInfoRepository {

    Integer upsertDateInfo(List<DateInfoReq> dateInfoReqs);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);

}
