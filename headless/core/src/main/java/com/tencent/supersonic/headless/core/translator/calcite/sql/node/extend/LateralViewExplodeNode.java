package com.tencent.supersonic.headless.core.translator.calcite.sql.node.extend;

import com.tencent.supersonic.headless.core.translator.calcite.sql.node.ExtendNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlWriter;

/**
 * extend node to handle lateral explode dataSet
 */
public class LateralViewExplodeNode extends ExtendNode {

    public final String sqlNameView = "view";
    public final String sqlNameExplode = "explode";
    public final String sqlNameExplodeSplit = "explode_split";
    private Map<String, String> delimiterMap;

    public LateralViewExplodeNode(Map<String, String> delimiterMap) {
        super();
        this.delimiterMap = delimiterMap;
    }

    public void unparse(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        SqlOperator operator = call.getOperator();
        writer.setNeedWhitespace(true);
        assert call.operandCount() == 2;
        writer.sep(SqlKind.SELECT.lowerName);
        writer.sep(SqlIdentifier.STAR.toString());
        writer.sep("from");
        SqlWriter.Frame frame = writer.startList(SqlWriter.FrameTypeEnum.SIMPLE);
        call.operand(0).unparse(writer, leftPrec, operator.getLeftPrec());
        writer.setNeedWhitespace(true);
        writer.sep(SqlKind.LATERAL.lowerName);
        writer.sep(sqlNameView);
        SqlNodeList list = (SqlNodeList) call.operand(1);
        Ord node;
        Iterator var = Ord.zip(list).iterator();
        while (var.hasNext()) {
            node = (Ord) var.next();
            if (node.i > 0 && node.i % 2 > 0) {
                writer.sep(SqlKind.AS.lowerName);
                ((SqlNode) node.e).unparse(writer, 0, 0);
                continue;
            }
            if (node.i > 0 && node.i % 2 == 0) {
                writer.sep(SqlKind.LATERAL.lowerName);
                writer.sep(sqlNameView);
            }
            explode(writer, (SqlNode) node.e);
        }
        writer.endList(frame);
    }

    public void explode(SqlWriter writer, SqlNode sqlNode) {
        String delimiter =
                Objects.nonNull(delimiterMap) && delimiterMap.containsKey(sqlNode.toString()) ? delimiterMap.get(
                        sqlNode.toString()) : "";
        if (delimiter.isEmpty()) {
            writer.sep(sqlNameExplode);
        } else {
            writer.sep(sqlNameExplodeSplit);
        }
        SqlWriter.Frame frame = writer.startList("(", ")");
        sqlNode.unparse(writer, 0, 0);
        if (!delimiter.isEmpty()) {
            writer.sep(",");
            writer.sep(String.format("'%s'", delimiter));
        }
        writer.endList(frame);
        writer.sep("tmp_sgl_" + sqlNode.toString());
    }
}
