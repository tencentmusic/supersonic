package com.tencent.supersonic.headless.model.infrastructure.mapper;


import com.tencent.supersonic.headless.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.model.domain.dataobject.DateInfoDO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DateInfoMapper {

    Boolean upsertDateInfo(DateInfoDO dateInfoDO);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);
}