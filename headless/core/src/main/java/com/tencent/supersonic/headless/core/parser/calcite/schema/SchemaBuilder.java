package com.tencent.supersonic.headless.core.parser.calcite.schema;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.parser.calcite.Configuration;
import com.tencent.supersonic.headless.core.parser.calcite.sql.S2SQLSqlValidatorImpl;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.prepare.Prepare;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.ParameterScope;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SchemaBuilder {

    public static final String MATERIALIZATION_SYS_DB = "SYS";
    public static final String MATERIALIZATION_SYS_SOURCE = "SYS_SOURCE";
    public static final String MATERIALIZATION_SYS_VIEW = "SYS_VIEW";
    public static final String MATERIALIZATION_SYS_FIELD_DATE = "C1";
    public static final String MATERIALIZATION_SYS_FIELD_DATA = "C2";

    public static SqlValidatorScope getScope(SemanticSchema schema) throws Exception {
        Map<String, RelDataType> nameToTypeMap = new HashMap<>();
        CalciteSchema rootSchema = CalciteSchema.createRootSchema(true, false);
        rootSchema.add(schema.getSchemaKey(), schema);
        Prepare.CatalogReader catalogReader = new CalciteCatalogReader(
                rootSchema,
                Collections.singletonList(schema.getSchemaKey()),
                Configuration.typeFactory,
                Configuration.config
        );
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        S2SQLSqlValidatorImpl s2SQLSqlValidator = new S2SQLSqlValidatorImpl(Configuration.operatorTable, catalogReader,
                Configuration.typeFactory, Configuration.getValidatorConfig(engineType));
        return new ParameterScope(s2SQLSqlValidator, nameToTypeMap);
    }

    public static CalciteSchema getMaterializationSchema() {
        CalciteSchema rootSchema = CalciteSchema.createRootSchema(true, false);
        SchemaPlus schema = rootSchema.plus().add(MATERIALIZATION_SYS_DB, new AbstractSchema());
        DataSourceTable srcTable = DataSourceTable.newBuilder(MATERIALIZATION_SYS_SOURCE)
                .addField(MATERIALIZATION_SYS_FIELD_DATE, SqlTypeName.DATE)
                .addField(MATERIALIZATION_SYS_FIELD_DATA, SqlTypeName.BIGINT)
                .withRowCount(1)
                .build();
        schema.add(MATERIALIZATION_SYS_SOURCE, srcTable);
        DataSourceTable dataSetTable = DataSourceTable.newBuilder(MATERIALIZATION_SYS_VIEW)
                .addField(MATERIALIZATION_SYS_FIELD_DATE, SqlTypeName.DATE)
                .addField(MATERIALIZATION_SYS_FIELD_DATA, SqlTypeName.BIGINT)
                .withRowCount(1)
                .build();
        schema.add(MATERIALIZATION_SYS_VIEW, dataSetTable);
        return rootSchema;
    }

    public static void addSourceView(CalciteSchema dataSetSchema, String dbSrc, String tbSrc, Set<String> dates,
            Set<String> dimensions, Set<String> metrics) {
        String tb = tbSrc.toLowerCase();
        String db = dbSrc.toLowerCase();
        DataSourceTable.Builder builder = DataSourceTable.newBuilder(tb);
        for (String date : dates) {
            builder.addField(date, SqlTypeName.VARCHAR);
        }
        for (String dim : dimensions) {
            builder.addField(dim, SqlTypeName.VARCHAR);
        }
        for (String metric : metrics) {
            builder.addField(metric, SqlTypeName.ANY);
        }
        DataSourceTable srcTable = builder
                .withRowCount(1)
                .build();
        if (Objects.nonNull(db) && !db.isEmpty()) {
            SchemaPlus schemaPlus = dataSetSchema.plus().getSubSchema(db);
            if (Objects.isNull(schemaPlus)) {
                dataSetSchema.plus().add(db, new AbstractSchema());
                schemaPlus = dataSetSchema.plus().getSubSchema(db);
            }
            schemaPlus.add(tb, srcTable);
        } else {
            dataSetSchema.add(tb, srcTable);
        }
    }
}
