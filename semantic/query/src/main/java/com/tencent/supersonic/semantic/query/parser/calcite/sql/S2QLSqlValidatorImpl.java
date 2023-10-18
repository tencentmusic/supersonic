package com.tencent.supersonic.semantic.query.parser.calcite.sql;

import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.validate.SqlValidatorCatalogReader;
import org.apache.calcite.sql.validate.SqlValidatorImpl;

public class S2QLSqlValidatorImpl extends SqlValidatorImpl {

    public S2QLSqlValidatorImpl(SqlOperatorTable opTab, SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory, Config config) {
        super(opTab, catalogReader, typeFactory, config);
    }
}
