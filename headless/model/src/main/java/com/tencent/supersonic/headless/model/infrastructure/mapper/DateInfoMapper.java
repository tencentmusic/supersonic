package com.tencent.supersonic.headless.model.infrastructure.mapper;


import com.tencent.supersonic.headless.common.model.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.model.domain.dataobject.DateInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DateInfoMapper {

    Boolean upsertDateInfo(DateInfoDO dateInfoDO);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);
}