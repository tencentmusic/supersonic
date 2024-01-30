package com.tencent.supersonic.headless.core.cache;


import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;

public interface QueryCache {

    Object query(SemanticQueryReq semanticQueryReq, String cacheKey);

    Boolean put(String cacheKey, Object value);

    String getCacheKey(SemanticQueryReq semanticQueryReq);

}
