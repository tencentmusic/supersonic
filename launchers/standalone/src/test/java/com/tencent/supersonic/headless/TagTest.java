package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.server.service.TagQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TagTest extends BaseTest {

    @Autowired
    private TagQueryService tagQueryService;

    @Test
    public void testQueryTagValue() throws Exception {
        ItemValueReq itemValueReq = new ItemValueReq();
        itemValueReq.setId(1L);
        ItemValueResp itemValueResp =
                tagQueryService.queryTagValue(itemValueReq, User.getDefaultUser());
        Assertions.assertNotNull(itemValueResp);
    }
}
