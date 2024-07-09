package com.tencent.supersonic.headless.server.facade.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;

import java.util.List;

/**
 * This interface abstracts functionalities provided by a semantic layer.
 */
public interface SemanticLayerService {

    DataSetSchema getDataSetSchema(Long id);

    SemanticTranslateResp translate(SemanticQueryReq queryReq, User user) throws Exception;

    SemanticQueryResp queryByReq(SemanticQueryReq queryReq, User user) throws Exception;

    SemanticQueryResp queryDimValue(QueryDimValueReq queryDimValueReq, User user);

    EntityInfo getEntityInfo(SemanticParseInfo parseInfo, DataSetSchema dataSetSchema, User user);

    List<ItemResp> getDomainDataSetTree();

}
