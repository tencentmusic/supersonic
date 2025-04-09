package com.tencent.supersonic.headless.server.modeller;


import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;

import java.util.List;

/**
 * A semantic modeler builds semantic-layer schemas from database-layer schemas.
 */
public interface SemanticModeller {

    void build(DbSchema dbSchema, List<DbSchema> otherDbSchema, ModelSchema modelSchema,
            ModelBuildReq modelBuildReq);

}
