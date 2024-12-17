package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class TableView {

    private Set<String> fields = Sets.newHashSet();
    private List<SqlNode> select = Lists.newArrayList();
    private SqlNodeList order;
    private SqlNode fetch;
    private SqlNode offset;
    private SqlNode table;
    private String alias;
    private List<String> primary;
    private ModelResp dataModel;

    public SqlNode build() {
        List<SqlNode> selectNodeList = new ArrayList<>();
        if (select.isEmpty()) {
            return new SqlSelect(SqlParserPos.ZERO, null,
                    new SqlNodeList(SqlNodeList.SINGLETON_STAR, SqlParserPos.ZERO), table, null,
                    null, null, null, null, order, offset, fetch, null);
        } else {
            selectNodeList.addAll(select);
            return new SqlSelect(SqlParserPos.ZERO, null,
                    new SqlNodeList(selectNodeList, SqlParserPos.ZERO), table, null, null, null,
                    null, null, order, offset, fetch, null);
        }
    }

}
