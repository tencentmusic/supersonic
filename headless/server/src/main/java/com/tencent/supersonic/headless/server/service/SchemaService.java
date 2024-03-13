package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.request.DataSetFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaItemQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface SchemaService {

    List<DataSetSchemaResp> fetchDataSetSchema(DataSetFilterReq filter);

    DataSetSchema getDataSetSchema(Long dataSetId);

    List<DataSetSchema> getDataSetSchema();

    List<ModelSchemaResp> fetchModelSchemaResps(List<Long> modelIds);

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq, User user);

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user);

    List querySchemaItem(SchemaItemQueryReq schemaItemQueryReq);

    List<DomainResp> getDomainList(User user);

    List<ModelResp> getModelList(User user, AuthType authType, Long domainId);

    SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) throws ExecutionException;

    List<ItemResp> getDomainDataSetTree();
}
