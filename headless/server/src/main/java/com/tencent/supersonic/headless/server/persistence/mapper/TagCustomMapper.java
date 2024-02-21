package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagCustomMapper {
    List<TagDO> query(TagFilter tagFilter);
}
