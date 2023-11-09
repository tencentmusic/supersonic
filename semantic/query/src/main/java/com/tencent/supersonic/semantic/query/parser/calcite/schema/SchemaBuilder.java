package com.tencent.supersonic.semantic.query.parser.calcite.schema;


import com.tencent.supersonic.semantic.query.parser.calcite.Configuration;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.S2SQLSqlValidatorImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.prepare.Prepare;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.ParameterScope;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class SchemaBuilder {

    public static final String MATERIALIZATION_SYS_DB = "SYS";
    public static final String MATERIALIZATION_SYS_SOURCE = "SYS_SOURCE";
    public static final String MATERIALIZATION_SYS_VIEW = "SYS_VIEW";
    public static final String MATERIALIZATION_SYS_FIELD_DATE = "C1";
    public static final String MATERIALIZATION_SYS_FIELD_DATA = "C2";


    public static SqlValidatorScope getScope(SemanticSchema schema) throws Exception {
        Map<String, RelDataType> nameToTypeMap = new HashMap<>();
        CalciteSchema rootSchema = CalciteSchema.createRootSchema(true, false);
        rootSchema.add(schema.getRootPath(), schema);
        Prepare.CatalogReader catalogReader = new CalciteCatalogReader(
                rootSchema,
                Collections.singletonList(schema.getRootPath()),
                Configuration.typeFactory,
                Configuration.config
        );
        S2SQLSqlValidatorImpl s2SQLSqlValidator = new S2SQLSqlValidatorImpl(Configuration.operatorTable, catalogReader,
                Configuration.typeFactory, Configuration.validatorConfig);
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
        DataSourceTable viewTable = DataSourceTable.newBuilder(MATERIALIZATION_SYS_VIEW)
                .addField(MATERIALIZATION_SYS_FIELD_DATE, SqlTypeName.DATE)
                .addField(MATERIALIZATION_SYS_FIELD_DATA, SqlTypeName.BIGINT)
                .withRowCount(1)
                .build();
        schema.add(MATERIALIZATION_SYS_VIEW, viewTable);
        return rootSchema;
    }
}
