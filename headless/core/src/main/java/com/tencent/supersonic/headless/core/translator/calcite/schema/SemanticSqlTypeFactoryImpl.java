package com.tencent.supersonic.headless.core.translator.calcite.schema;

import java.nio.charset.Charset;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;

/**
 * customize the  SqlTypeFactoryImpl
 */
public class SemanticSqlTypeFactoryImpl extends SqlTypeFactoryImpl {

    public SemanticSqlTypeFactoryImpl(RelDataTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public Charset getDefaultCharset() {
        return Charset.forName("UTF8");
    }
}
