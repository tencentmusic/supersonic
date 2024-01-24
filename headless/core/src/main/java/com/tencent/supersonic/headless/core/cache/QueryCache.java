package com.tencent.supersonic.headless.core.cache;


import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;

public interface QueryCache {

    Object query(SemanticQueryReq semanticQueryReq);

    Boolean put(SemanticQueryReq semanticQueryReq, Object value);

    String getCacheKey(SemanticQueryReq semanticQueryReq);

}
