package com.tencent.supersonic.common.calcite;

import org.apache.calcite.sql.dialect.MysqlSqlDialect;

public class S2MysqlSqlDialect extends MysqlSqlDialect {

    public S2MysqlSqlDialect(Context context) {
        super(context);
    }

    @Override
    public void quoteStringLiteral(StringBuilder buf, String charsetName, String val) {
        buf.append(this.literalQuoteString);
        buf.append(val.replace(this.literalEndQuoteString, this.literalEscapedQuote));
        buf.append(this.literalEndQuoteString);
    }
}