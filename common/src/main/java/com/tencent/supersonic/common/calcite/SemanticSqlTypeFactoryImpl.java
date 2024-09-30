package com.tencent.supersonic.common.calcite;

import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;

import java.nio.charset.Charset;

/** customize the SqlTypeFactoryImpl */
public class SemanticSqlTypeFactoryImpl extends SqlTypeFactoryImpl {

    public SemanticSqlTypeFactoryImpl(RelDataTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public Charset getDefaultCharset() {
        return Charset.forName("UTF8");
    }
}
