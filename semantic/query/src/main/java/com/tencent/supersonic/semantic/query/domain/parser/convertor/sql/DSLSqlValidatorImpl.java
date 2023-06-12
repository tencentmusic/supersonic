package com.tencent.supersonic.semantic.query.domain.parser.convertor.sql;

import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.validate.SqlValidatorCatalogReader;
import org.apache.calcite.sql.validate.SqlValidatorImpl;

public class DSLSqlValidatorImpl extends SqlValidatorImpl {

    public DSLSqlValidatorImpl(SqlOperatorTable opTab, SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory, Config config) {
        super(opTab, catalogReader, typeFactory, config);
    }
}
