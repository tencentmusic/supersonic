package com.tencent.supersonic.headless.server.persistence.repository;


import com.tencent.supersonic.headless.api.pojo.request.TagDeleteReq;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import java.util.List;


public interface TagRepository {

    Long create(TagDO tagDO);

    void update(TagDO tagDO);

    TagDO getTagById(Long id);

    List<TagDO> getTagDOList(TagFilter tagFilter);

    List<TagResp> queryTagRespList(TagFilter tagFilter);

    Boolean delete(Long id);

    void deleteBatch(TagDeleteReq tagDeleteReq);
}
