package com.tencent.supersonic.headless.query.parser.calcite.schema;

import java.nio.charset.Charset;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;

public class HeadlessSqlTypeFactoryImpl extends SqlTypeFactoryImpl {

    public HeadlessSqlTypeFactoryImpl(RelDataTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public Charset getDefaultCharset() {
        return Charset.forName("UTF8");
    }
}
