package com.tencent.supersonic.semantic.query.domain.parser.schema;


import com.tencent.supersonic.semantic.query.domain.parser.convertor.Configuration;
import com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.DSLSqlValidatorImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.prepare.Prepare;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.validate.ParameterScope;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class SchemaBuilder {

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
        DSLSqlValidatorImpl dslSqlValidator = new DSLSqlValidatorImpl(Configuration.operatorTable, catalogReader,
                Configuration.typeFactory, Configuration.validatorConfig);
        return new ParameterScope(dslSqlValidator, nameToTypeMap);
    }
}
