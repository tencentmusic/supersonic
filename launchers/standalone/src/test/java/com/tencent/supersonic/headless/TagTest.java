package com.tencent.supersonic.headless;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.TagDefineParams;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.service.TagQueryService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;

public class TagTest extends BaseTest {

    private final String bizName = "page";
    private final Long modelId = 3L;
    private final Integer dimId = 3;

    @Autowired
    private TagMetaService tagMetaService;
    @Autowired
    private TagQueryService tagQueryService;

    @Test
    void testCreateTag() {
        TagReq tagReq = newTagReq();
        tagMetaService.create(tagReq, User.getFakeUser());

        TagResp tag = queryTagRespByBizName(bizName);
        Assert.assertEquals(bizName, tag.getBizName());
        tagMetaService.delete(tag.getId(), User.getFakeUser());
    }

    @Test
    void testUpdateTag() {
        TagReq tagReq = newTagReq();
        TagResp tagResp = tagMetaService.create(tagReq, User.getFakeUser());
        Assert.assertEquals(bizName, tagReq.getBizName());
        tagReq.setId(tagResp.getId());
        tagReq.setName("新页面");
        tagMetaService.update(tagReq, User.getFakeUser());

        TagResp tag = queryTagRespByBizName(bizName);
        Assert.assertEquals("新页面", tag.getName());
        tagMetaService.delete(tag.getId(), User.getFakeUser());
    }

    private TagResp queryTagRespByBizName(String bizName) {
        TagFilter tagFilter = new TagFilter();
        tagFilter.setBizName(bizName);
        TagResp tagRespDb = tagMetaService.getTags(tagFilter).get(0);
        return tagRespDb;
    }

    private TagReq newTagReq() {
        TagReq tagReq = new TagReq();
        tagReq.setModelId(modelId);
        tagReq.setName("页面");
        tagReq.setBizName(bizName);
        tagReq.setStatus(1);
        tagReq.setTypeEnum(TypeEnums.TAG);
        tagReq.setTagDefineType(TagDefineType.DIMENSION);

        TagDefineParams tagDefineParams = new TagDefineParams();
        tagDefineParams.setExpr(bizName);
        tagDefineParams.setDependencies(new ArrayList<>(Arrays.asList(dimId)));
        tagReq.setTagDefineParams(tagDefineParams);
        return tagReq;
    }

    @Test
    void testQueryTag() {
        TagReq tagReq = newTagReq();
        tagMetaService.create(tagReq, User.getFakeUser());
        TagResp tag = queryTagRespByBizName(bizName);
        Assert.assertEquals(bizName, tag.getBizName());
        tagMetaService.delete(tag.getId(), User.getFakeUser());
    }

    @Test
    void testTagValue() throws Exception {
        TagReq tagReq = newTagReq();
        tagMetaService.create(tagReq, User.getFakeUser());
        TagResp tag = queryTagRespByBizName(bizName);
        ItemValueReq itemValueReq = new ItemValueReq();
        itemValueReq.setItemId(tag.getId());
        tagMetaService.delete(tag.getId(), User.getFakeUser());
    }

}