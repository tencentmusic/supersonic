package com.tencent.supersonic.headless.core.translator.parser.calcite;

import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.validate.SqlValidatorCatalogReader;
import org.apache.calcite.sql.validate.SqlValidatorImpl;

/** customize the SqlValidatorImpl */
public class S2SQLSqlValidatorImpl extends SqlValidatorImpl {

    public S2SQLSqlValidatorImpl(SqlOperatorTable opTab, SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory, Config config) {
        super(opTab, catalogReader, typeFactory, config);
    }
}
