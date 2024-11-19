package com.tencent.supersonic.headless.chat.query;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import org.apache.calcite.sql.parser.SqlParseException;

/** A semantic query executes specific type of query based on the results of semantic parsing. */
public interface SemanticQuery {

    String getQueryMode();

    SemanticQueryReq buildSemanticQueryReq() throws SqlParseException;

    void buildS2Sql(DataSetSchema dataSetSchema);

    SemanticParseInfo getParseInfo();

    void setParseInfo(SemanticParseInfo parseInfo);
}
