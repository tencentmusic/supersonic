package com.tencent.supersonic.headless.server.facade.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;

import java.util.List;

/** This interface abstracts functionalities provided by a semantic layer. */
public interface SemanticLayerService {

    SemanticTranslateResp translate(SemanticQueryReq queryReq, User user) throws Exception;

    SemanticQueryResp queryByReq(SemanticQueryReq queryReq, User user) throws Exception;

    SemanticQueryResp queryDimensionValue(DimensionValueReq dimensionValueReq, User user);

    DataSetSchema getDataSetSchema(Long id);

    List<ItemResp> getDomainDataSetTree();

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    List<MetricResp> getMetrics(MetaFilter metaFilter);
}
