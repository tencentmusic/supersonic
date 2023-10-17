package com.tencent.supersonic.semantic.materialization.domain.repository;

import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationConfFilter;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationElement;

import java.util.List;

public interface MaterializationElementRepository {
    Boolean insert(MaterializationElement materializationElement);

    Boolean update(MaterializationElement materializationElement);

    List<MaterializationElementResp> getMaterializationElementResp(MaterializationConfFilter filter);

    Boolean cleanMaterializationElement(Long materializationId);
}
