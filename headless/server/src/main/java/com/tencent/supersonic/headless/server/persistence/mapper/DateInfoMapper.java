package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.server.persistence.dataobject.DateInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DateInfoMapper extends BaseMapper<DateInfoDO> {

    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);
}
