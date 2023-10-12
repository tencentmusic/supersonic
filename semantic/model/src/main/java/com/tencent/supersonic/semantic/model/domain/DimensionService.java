package com.tencent.supersonic.semantic.model.domain;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import java.util.List;

public interface DimensionService {

    List<DimensionResp> getDimensions(List<Long> ids);

    List<DimensionResp> getDimensions(Long domainId);

    List<DimensionResp> getDimensions();

    DimensionResp getDimension(Long id);

    DimensionResp getDimension(String bizName, Long modelId);

    List<DimensionResp> getDimensionsByModelIds(List<Long> modelIds);

    void createDimension(DimensionReq dimensionReq, User user) throws Exception;

    void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) throws Exception;

    List<DimensionResp> getDimensionsByDatasource(Long datasourceId);

    void updateDimension(DimensionReq dimensionReq, User user) throws Exception;

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq);

    List<DimensionResp> getHighSensitiveDimension(Long domainId);

    List<DimensionResp> getAllHighSensitiveDimension();

    void deleteDimension(Long id) throws Exception;

    List<String> mockAlias(DimensionReq dimensionReq, String mockType, User user);

    List<DimValueMap> mockDimensionValueAlias(DimensionReq dimensionReq, User user);
}
