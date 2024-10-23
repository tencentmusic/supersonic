package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.TagObjectReq;
import com.tencent.supersonic.headless.api.pojo.response.TagObjectResp;
import com.tencent.supersonic.headless.server.pojo.TagObjectFilter;
import com.tencent.supersonic.headless.server.service.TagObjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class TagObjectTest extends BaseTest {

    @Autowired
    private TagObjectService tagObjectService;

    @Test
    void testCreateTagObject() throws Exception {
        User user = User.getDefaultUser();
        TagObjectReq tagObjectReq = newTagObjectReq();
        TagObjectResp tagObjectResp = tagObjectService.create(tagObjectReq, user);
        tagObjectService.delete(tagObjectResp.getId(), user, false);
    }

    @Test
    void testUpdateTagObject() throws Exception {
        TagObjectReq tagObjectReq = newTagObjectReq();
        TagObjectResp tagObjectResp = tagObjectService.create(tagObjectReq, User.getDefaultUser());
        TagObjectReq tagObjectReqUpdate = new TagObjectReq();
        BeanUtils.copyProperties(tagObjectResp, tagObjectReqUpdate);
        tagObjectReqUpdate.setName("艺人1");
        tagObjectService.update(tagObjectReqUpdate, User.getDefaultUser());
        TagObjectResp tagObject =
                tagObjectService.getTagObject(tagObjectReqUpdate.getId(), User.getDefaultUser());
        tagObjectService.delete(tagObject.getId(), User.getDefaultUser(), false);
    }

    @Test
    void testQueryTagObject() throws Exception {
        TagObjectReq tagObjectReq = newTagObjectReq();
        TagObjectResp tagObjectResp = tagObjectService.create(tagObjectReq, User.getDefaultUser());
        TagObjectFilter filter = new TagObjectFilter();
        List<TagObjectResp> tagObjects =
                tagObjectService.getTagObjects(filter, User.getDefaultUser());
        tagObjects.size();
        tagObjectService.delete(tagObjectResp.getId(), User.getDefaultUser(), false);
    }

    private TagObjectReq newTagObjectReq() {
        TagObjectReq tagObjectReq = new TagObjectReq();
        tagObjectReq.setDomainId(2L);
        tagObjectReq.setName("新艺人");
        tagObjectReq.setBizName("new_singer");
        return tagObjectReq;
    }
}
