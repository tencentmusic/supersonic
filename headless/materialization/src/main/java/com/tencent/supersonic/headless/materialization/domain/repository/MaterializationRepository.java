package com.tencent.supersonic.headless.materialization.domain.repository;

import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationResp;
import com.tencent.supersonic.headless.materialization.domain.pojo.Materialization;

import java.util.List;

public interface MaterializationRepository {
    Boolean insert(Materialization materialization);

    Boolean update(Materialization materialization);

    List<MaterializationResp> getMaterializationResp(MaterializationFilter filter);

    MaterializationResp getMaterialization(Long id);
}
