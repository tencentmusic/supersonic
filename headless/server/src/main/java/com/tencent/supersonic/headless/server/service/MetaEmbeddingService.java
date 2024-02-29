package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import java.util.List;

public interface MetaEmbeddingService {

    List<RetrieveQueryResult> retrieveQuery(List<Long> dataSetIds, RetrieveQuery retrieveQuery, int num);

}
