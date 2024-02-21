package com.tencent.supersonic.headless.server.persistence.repository;


import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import java.util.List;


public interface TagRepository {

    Long create(TagDO tagDO);

    void update(TagDO tagDO);

    TagDO getTagById(Long id);

    List<TagDO> query(TagFilter tagFilter);
}
