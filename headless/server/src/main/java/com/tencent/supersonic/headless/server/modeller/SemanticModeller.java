package com.tencent.supersonic.headless.server.modeller;


import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;

import java.util.List;

public interface SemanticModeller {

    ModelSchema build(DbSchema dbSchema, List<DbSchema> otherDbSchema, ModelBuildReq modelBuildReq);

}
