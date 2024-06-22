package com.tencent.supersonic.headless.core.translator.calcite.sql.node;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlInternalOperator;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.SqlWriter.Frame;
import org.apache.calcite.sql.SqlWriter.FrameTypeEnum;

public class ExtendNode extends SqlInternalOperator {

    public ExtendNode() {
        super(SqlKind.EXTEND.lowerName, SqlKind.EXTEND);
    }

    public void unparse(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        SqlOperator operator = call.getOperator();
        Frame frame = writer.startList(FrameTypeEnum.SIMPLE);
        call.operand(0).unparse(writer, leftPrec, operator.getLeftPrec());
        writer.setNeedWhitespace(true);
        writer.sep(operator.getName());
        SqlNodeList list = (SqlNodeList) call.operand(1);
        Frame frameArgs = writer.startList("(", ")");
        for (int i = 0; i < list.size(); i++) {
            list.get(i).unparse(writer, 0, 0);
            if (i < list.size() - 1) {
                writer.sep(",");
            }
        }
        writer.endList(frameArgs);
        writer.endList(frame);
    }
}