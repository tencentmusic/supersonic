package com.tencent.supersonic.headless.server.modeller;

import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.SemanticColumn;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RuleSemanticModeller implements SemanticModeller {

    @Override
    public void build(DbSchema dbSchema, List<DbSchema> dbSchemas, ModelSchema modelSchema,
            ModelBuildReq modelBuildReq) {
        List<SemanticColumn> semanticColumns =
                dbSchema.getDbColumns().stream().map(this::convert).collect(Collectors.toList());
        modelSchema.setSemanticColumns(semanticColumns);
    }

    private SemanticColumn convert(DBColumn dbColumn) {
        SemanticColumn semanticColumn = new SemanticColumn();
        semanticColumn.setName(dbColumn.getColumnName());
        semanticColumn.setColumnName(dbColumn.getColumnName());
        semanticColumn.setExpr(dbColumn.getColumnName());
        semanticColumn.setComment(dbColumn.getComment());
        semanticColumn.setDataType(dbColumn.getDataType());
        semanticColumn.setFiledType(dbColumn.getFieldType());
        return semanticColumn;
    }

}
