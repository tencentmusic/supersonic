package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagCustomMapper {

    List<TagResp> queryTagRespList(TagFilter tagFilter);

    List<TagDO> getTagDOList(TagFilter tagFilter);

    Boolean deleteById(Long id);

    void deleteBatchByIds(List<Long> ids);

    void deleteBatchByType(List<Long> itemIds, String type);
}
