package com.tencent.supersonic.headless;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.request.TagDeleteReq;
import com.tencent.supersonic.headless.api.pojo.request.TagFilterPageReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.web.service.TagMetaService;
import com.tencent.supersonic.headless.server.web.service.TagQueryService;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TagTest extends BaseTest {

    @Autowired
    private TagMetaService tagMetaService;

    @Autowired
    private TagQueryService tagQueryService;

    private User user = User.getFakeUser();

    public TagReq newTagReq() {
        TagReq tagReq = new TagReq();
        tagReq.setTagDefineType(TagDefineType.DIMENSION);
        tagReq.setItemId(4L);
        return tagReq;
    }

    public TagReq newTagReqV1() {
        TagReq tagReq = new TagReq();
        tagReq.setTagDefineType(TagDefineType.DIMENSION);
        tagReq.setItemId(5L);
        return tagReq;
    }

    public TagReq newTagReqV2() {
        TagReq tagReq = new TagReq();
        tagReq.setTagDefineType(TagDefineType.DIMENSION);
        tagReq.setItemId(1L);
        return tagReq;
    }

    public void setUp() {
        TagDeleteReq tagDeleteReq = new TagDeleteReq();
        tagDeleteReq.setTagDefineType(TagDefineType.DIMENSION);
        tagDeleteReq.setItemIds(Arrays.asList(1L, 4L, 5L));
        List<TagDeleteReq> tagDeleteReqList = new ArrayList<>();
        tagDeleteReqList.add(tagDeleteReq);
        tagMetaService.deleteBatch(tagDeleteReqList, user);
    }

    @Order(1)
    @Test
    public void testCreateTag() {
        setUp();
        TagReq tagReq = newTagReq();
        tagMetaService.create(tagReq, user);
        TagReq tagReq1 = newTagReqV1();
        tagMetaService.create(tagReq1, user);
        TagFilter tagFilter = new TagFilter();
        tagFilter.setItemIds(Arrays.asList(4L, 5L));
        tagFilter.setTagDefineType(TagDefineType.DIMENSION);
        List<TagResp> tags = tagMetaService.getTags(tagFilter);
        Assert.assertEquals(2, tags.size());
        TagDeleteReq tagDeleteReq = new TagDeleteReq();
        tagDeleteReq.setTagDefineType(TagDefineType.DIMENSION);
        tagDeleteReq.setItemIds(Arrays.asList(4L, 5L));
        List<TagDeleteReq> tagDeleteReqList = new ArrayList<>();
        tagDeleteReqList.add(tagDeleteReq);
        tagMetaService.deleteBatch(tagDeleteReqList, user);
        List<TagResp> tags1 = tagMetaService.getTags(tagFilter);
        Assert.assertEquals(0, tags1.size());
    }

    @Order(2)
    @Test
    public void testTagMarket() {
        setUp();
        TagReq tagReq = newTagReq();
        tagMetaService.create(tagReq, user);
        TagReq tagReq1 = newTagReqV1();
        tagMetaService.create(tagReq1, user);
        TagReq tagReq2 = newTagReqV2();
        tagMetaService.create(tagReq2, user);
        TagFilterPageReq filter = new TagFilterPageReq();
        filter.setTagObjectId(2L);
        filter.setKey("区域");
        PageInfo<TagResp> tags = tagMetaService.queryTagMarketPage(filter, user);
        Assert.assertEquals(1, tags.getList().size());
        TagDeleteReq tagDeleteReq = new TagDeleteReq();
        tagDeleteReq.setTagDefineType(TagDefineType.DIMENSION);
        tagDeleteReq.setItemIds(Arrays.asList(1L, 4L, 5L));
        List<TagDeleteReq> tagDeleteReqList = new ArrayList<>();
        tagDeleteReqList.add(tagDeleteReq);
        tagMetaService.deleteBatch(tagDeleteReqList, user);
    }

    @Test
    @Order(0)
    public void testQueryTagValue() throws Exception {
        ItemValueReq itemValueReq = new ItemValueReq();
        itemValueReq.setId(1L);
        ItemValueResp itemValueResp = tagQueryService.queryTagValue(itemValueReq, user);
        Assertions.assertNotNull(itemValueResp);
    }

}