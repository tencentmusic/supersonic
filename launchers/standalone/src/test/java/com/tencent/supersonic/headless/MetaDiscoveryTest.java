package com.tencent.supersonic.headless;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.server.service.MetaDiscoveryService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

public class MetaDiscoveryTest extends BaseTest {

    @Autowired
    protected MetaDiscoveryService metaDiscoveryService;

    @Test
    public void testGetMapMeta() throws Exception {
        QueryMapReq queryMapReq = new QueryMapReq();
        queryMapReq.setQueryText("对比alice和lucy的访问次数");
        queryMapReq.setTopN(10);
        queryMapReq.setUser(User.getFakeUser());
        queryMapReq.setDataSetNames(Collections.singletonList("超音数"));
        MapInfoResp mapMeta = metaDiscoveryService.getMapMeta(queryMapReq);

        Assert.assertNotNull(mapMeta);
        Assert.assertNotEquals(0, mapMeta.getMapFields());
        Assert.assertNotEquals(0, mapMeta.getTopFields());
    }
}
