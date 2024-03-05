package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.pojo.TagFilterPage;
import java.util.List;

public interface TagService {

    TagResp create(TagReq tagReq, User user);

    TagResp update(TagReq tagReq, User user);

    void delete(Long id, User user);

    TagResp getTag(Long id, User user);

    List<TagResp> getTags(TagFilter tagFilter);

    PageInfo<TagResp> queryPage(TagFilterPage tagFilterPage, User user);

    Boolean batchUpdateStatus(MetaBatchReq metaBatchReq, User user);
}
