package com.tencent.supersonic.headless.server.modeller;

import com.tencent.supersonic.headless.api.pojo.ColumnSchema;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RuleSemanticModeller implements SemanticModeller {

    @Override
    public void build(DbSchema dbSchema, List<DbSchema> dbSchemas, ModelSchema modelSchema,
            ModelBuildReq modelBuildReq) {
        List<ColumnSchema> columnSchemas =
                dbSchema.getDbColumns().stream().map(this::convert).collect(Collectors.toList());
        modelSchema.setColumnSchemas(columnSchemas);
    }

    private ColumnSchema convert(DBColumn dbColumn) {
        ColumnSchema columnSchema = new ColumnSchema();
        columnSchema.setName(dbColumn.getColumnName());
        columnSchema.setColumnName(dbColumn.getColumnName());
        columnSchema.setComment(dbColumn.getComment());
        columnSchema.setDataType(dbColumn.getDataType());
        columnSchema.setFiledType(dbColumn.getFieldType());
        return columnSchema;
    }

}
