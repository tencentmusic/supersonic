package com.tencent.supersonic.semantic.core.domain;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.DimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import java.util.List;

public interface DimensionService {

    List<DimensionResp> getDimensions(List<Long> ids);

    List<DimensionResp> getDimensions(Long domainId);

    DimensionResp getDimension(String bizName, Long domainId);

    void createDimension(DimensionReq dimensionReq, User user) throws Exception;

    void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) throws Exception;

    List<DimensionResp> getDimensionsByDatasource(Long datasourceId);

    void updateDimension(DimensionReq dimensionReq, User user) throws Exception;

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq);

    List<DimensionResp> getHighSensitiveDimension(Long domainId);

    List<DimensionResp> getAllHighSensitiveDimension();

    void deleteDimension(Long id) throws Exception;
}
