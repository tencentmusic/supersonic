package com.tencent.supersonic.semantic.query.application.parser.calcite.sql.node;

import com.tencent.supersonic.semantic.query.application.parser.calcite.dsl.Identify;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class IdentifyNode extends SemanticNode {

    public static SqlNode build(Identify identify, SqlValidatorScope scope) throws Exception {
        return parse(identify.getName(), scope);
    }
}
