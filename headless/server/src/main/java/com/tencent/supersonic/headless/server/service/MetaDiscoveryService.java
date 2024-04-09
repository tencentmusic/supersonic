package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;

public interface MetaDiscoveryService {

    MapInfoResp getMapMeta(QueryMapReq queryMapReq);

}