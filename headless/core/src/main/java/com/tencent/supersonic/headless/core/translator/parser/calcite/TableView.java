package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.headless.core.pojo.DataModel;
import lombok.Data;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** basic query project */
@Data
public class TableView {

    private Set<String> fields = Sets.newHashSet();
    private List<SqlNode> dimension = new ArrayList<>();
    private List<SqlNode> metric = new ArrayList<>();
    private SqlNodeList order;
    private SqlNode fetch;
    private SqlNode offset;
    private SqlNode table;
    private List<SqlNode> select = Lists.newArrayList();

    private String alias;
    private List<String> primary;
    private DataModel dataModel;

    public SqlNode build() {
        List<SqlNode> selectNodeList = new ArrayList<>();
        selectNodeList.addAll(metric);
        selectNodeList.addAll(dimension);
        selectNodeList.addAll(select);
        return new SqlSelect(SqlParserPos.ZERO, null,
                new SqlNodeList(selectNodeList, SqlParserPos.ZERO), table, null, null, null, null,
                null, order, offset, fetch, null);
    }

}
