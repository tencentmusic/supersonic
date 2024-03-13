package com.tencent.supersonic.headless;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.ItemValueConfig;
import com.tencent.supersonic.headless.api.pojo.TagDefineParams;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.service.DictConfService;
import com.tencent.supersonic.headless.server.service.DictTaskService;
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
    @Autowired
    private DictConfService dictConfService;
    @Autowired
    private DictTaskService dictTaskService;

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
        // tagQueryService.queryTagValue(itemValueReq, User.getFakeUser());
        tagMetaService.delete(tag.getId(), User.getFakeUser());
    }

    @Test
    void testTagDict() {
        User user = User.getFakeUser();
        TagReq tagReq = newTagReq();
        TagResp tagResp = tagMetaService.create(tagReq, user);
        // add conf
        DictItemReq itemValueReq = new DictItemReq();
        itemValueReq.setType(TypeEnums.TAG);
        itemValueReq.setItemId(tagResp.getId());
        itemValueReq.setStatus(StatusEnum.ONLINE);
        ItemValueConfig config = new ItemValueConfig();
        config.setMetricId(4L);
        config.setWhiteList(Arrays.asList("p10", "p20"));
        config.setBlackList(Arrays.asList("p1", "p2"));
        itemValueReq.setConfig(config);
        dictConfService.addDictConf(itemValueReq, user);
        // run Task
        DictSingleTaskReq taskReq = DictSingleTaskReq.builder().type(TypeEnums.TAG).itemId(tagResp.getId()).build();
        dictTaskService.addDictTask(taskReq, user);

        tagMetaService.delete(tagResp.getId(), user);
    }

}