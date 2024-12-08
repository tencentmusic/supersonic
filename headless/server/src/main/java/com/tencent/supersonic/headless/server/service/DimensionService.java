package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.request.DimValueAliasReq;
import com.tencent.supersonic.headless.api.pojo.request.DimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.server.pojo.DimensionsFilter;

import java.util.List;

public interface DimensionService {

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    DimensionResp getDimension(String bizName, Long modelId);

    DimensionResp getDimension(Long id);

    void batchUpdateStatus(MetaBatchReq metaBatchReq, User user);

    DimensionResp createDimension(DimensionReq dimensionReq, User user) throws Exception;

    void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) throws Exception;

    void updateDimension(DimensionReq dimensionReq, User user) throws Exception;

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq);

    List<DimensionResp> queryDimensions(DimensionsFilter dimensionsFilter);

    void batchUpdateSensitiveLevel(MetaBatchReq metaBatchReq, User user);

    void deleteDimension(Long id, User user);

    List<DimensionResp> getDimensionInModelCluster(Long modelId);

    List<String> mockAlias(DimensionReq dimensionReq, String mockType, User user);

    List<DimValueMap> mockDimensionValueAlias(DimensionReq dimensionReq, User user);

    void sendDimensionEventBatch(List<Long> modelIds, EventType eventType);

    DataEvent getAllDataEvents();

    Boolean updateDimValueAlias(DimValueAliasReq req, User user);
}
