package com.tencent.supersonic.headless.materialization.domain.repository;

import com.tencent.supersonic.headless.common.materialization.pojo.MaterializationConfFilter;
import com.tencent.supersonic.headless.common.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.headless.materialization.domain.pojo.MaterializationElement;

import java.util.List;

public interface MaterializationElementRepository {
    Boolean insert(MaterializationElement materializationElement);

    Boolean update(MaterializationElement materializationElement);

    List<MaterializationElementResp> getMaterializationElementResp(MaterializationConfFilter filter);

    Boolean cleanMaterializationElement(Long materializationId);
}
