package com.tencent.supersonic.headless;

import static org.junit.Assert.assertTrue;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;
import com.tencent.supersonic.headless.server.service.TagQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryTagValueTest extends BaseTest {

    @Autowired
    protected TagQueryService tagQueryService;

    @Test
    public void testQueryTagValue() throws Exception {
        ItemValueReq itemValueReq = new ItemValueReq();
        itemValueReq.setId(1L);
        ItemValueResp itemValueResp = tagQueryService.queryTagValue(itemValueReq, User.getFakeUser());
        assertTrue(itemValueResp != null);
    }
}
