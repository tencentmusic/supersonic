package com.tencent.supersonic.headless.server.persistence.repository;


import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.request.DateInfoReq;
import com.tencent.supersonic.headless.server.persistence.dataobject.DateInfoDO;

import java.util.List;

public interface DateInfoRepository {

    Integer upsertDateInfo(List<DateInfoReq> dateInfoReqs);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);

}
