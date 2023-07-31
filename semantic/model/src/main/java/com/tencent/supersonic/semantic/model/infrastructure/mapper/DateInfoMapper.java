package com.tencent.supersonic.semantic.model.infrastructure.mapper;


import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.model.domain.dataobject.DateInfoDO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DateInfoMapper {

    Boolean upsertDateInfo(DateInfoDO dateInfoDO);

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);
}