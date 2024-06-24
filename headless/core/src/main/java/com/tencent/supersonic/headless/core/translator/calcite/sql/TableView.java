package com.tencent.supersonic.headless.core.translator.calcite.sql;

import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;

/**
 * basic query project
 */
@Data
public class TableView {

    private List<SqlNode> filter = new ArrayList<>();
    private List<SqlNode> dimension = new ArrayList<>();
    private List<SqlNode> measure = new ArrayList<>();
    private SqlNodeList order;
    private SqlNode fetch;
    private SqlNode offset;
    private SqlNode table;

    private String alias;
    private List<String> primary;
    private DataSource dataSource;

    public SqlNode build() {
        measure.addAll(dimension);
        SqlNodeList dimensionNodeList = null;
        if (dimension.size() > 0) {
            dimensionNodeList = new SqlNodeList(getGroup(dimension), SqlParserPos.ZERO);
        }
        SqlNodeList filterNodeList = null;
        if (filter.size() > 0) {
            filterNodeList = new SqlNodeList(filter, SqlParserPos.ZERO);
        }
        return new SqlSelect(SqlParserPos.ZERO, null, new SqlNodeList(measure, SqlParserPos.ZERO), table,
                filterNodeList, dimensionNodeList, null, null, null, order, offset, fetch, null);
    }

    private List<SqlNode> getGroup(List<SqlNode> sqlNodeList) {
        return sqlNodeList.stream()
                .map(s -> (s.getKind().equals(SqlKind.AS) ? ((SqlBasicCall) s).getOperandList().get(0) : s))
                .collect(
                        Collectors.toList());
    }

}
