package com.tencent.supersonic.headless.materialization.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.materialization.pojo.MaterializationConfFilter;
import com.tencent.supersonic.headless.api.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.headless.api.materialization.request.MaterializationElementReq;
import com.tencent.supersonic.headless.api.materialization.request.MaterializationReq;
import com.tencent.supersonic.headless.api.materialization.response.MaterializationElementModelResp;
import com.tencent.supersonic.headless.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.headless.api.materialization.response.MaterializationSourceResp;
import com.tencent.supersonic.headless.api.model.response.MeasureResp;
import java.util.List;

public interface MaterializationConfService {

    Boolean addMaterializationConf(MaterializationReq materializationReq, User user);

    Boolean updateMaterializationConf(MaterializationReq materializationReq, User user);

    List<MaterializationResp> getMaterializationResp(MaterializationFilter filter, User user);

    Boolean addMaterializationElementConf(MaterializationElementReq materializationElementReq, User user);

    Boolean updateMaterializationElementConf(MaterializationElementReq materializationElementReq, User user);

    List<MaterializationResp> queryMaterializationConf(MaterializationConfFilter filter, User user);

    List<MaterializationResp> getMaterializationByModel(Long modelId);

    List<Long> getMaterializationByTable(Long databaseId, String destinationTable);

    String generateCreateSql(Long materializationId, User user);

    Boolean initMaterializationElementConf(MaterializationConfFilter filter, User user);

    List<MaterializationElementModelResp> getMaterializationElementModels(Long materializationId, User user);

    List<MaterializationSourceResp> getMaterializationSourceResp(Long materializationId);

    Long getSourceIdByMeasure(List<MeasureResp> measureRespList, String bizName);

}
