package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.TagDeleteReq;
import com.tencent.supersonic.headless.api.pojo.request.TagFilterPageReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.TagItem;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;

import java.util.List;

public interface TagMetaService {

    TagResp create(TagReq tagReq, User user);

    Integer createBatch(List<TagReq> tagReqList, User user);

    Boolean delete(Long id, User user);

    Boolean deleteBatch(List<TagDeleteReq> tagDeleteReqList, User user);

    TagResp getTag(Long id, User user);

    List<TagResp> getTags(TagFilter tagFilter);

    List<TagDO> getTagDOList(TagFilter tagFilter);

    PageInfo<TagResp> queryTagMarketPage(TagFilterPageReq tagMarketPageReq, User user);

    List<TagItem> getTagItems(List<Long> itemIds, TagDefineType tagDefineType);
}
