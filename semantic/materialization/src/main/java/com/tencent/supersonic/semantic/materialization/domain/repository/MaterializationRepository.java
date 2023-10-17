package com.tencent.supersonic.semantic.materialization.domain.repository;

import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.materialization.domain.pojo.Materialization;

import java.util.List;

public interface MaterializationRepository {
    Boolean insert(Materialization materialization);

    Boolean update(Materialization materialization);

    List<MaterializationResp> getMaterializationResp(MaterializationFilter filter);

    MaterializationResp getMaterialization(Long id);
}
