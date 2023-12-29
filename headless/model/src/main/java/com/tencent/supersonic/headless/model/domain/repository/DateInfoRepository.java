package com.tencent.supersonic.headless.model.domain.repository;


import com.tencent.supersonic.headless.common.model.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.common.model.request.DateInfoReq;
import com.tencent.supersonic.headless.model.domain.dataobject.DateInfoDO;

import java.util.List;

public interface DateInfoRepository {

    Integer upsertDateInfo(List<DateInfoReq> dateInfoReqs);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);

}
