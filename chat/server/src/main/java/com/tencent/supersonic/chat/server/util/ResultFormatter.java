package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.common.pojo.QueryColumn;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public class ResultFormatter {

    public static String transform2TextNew(List<QueryColumn> queryColumns,
            List<Map<String, Object>> queryResults) {
        if (CollectionUtils.isEmpty(queryColumns)) {
            return "";
        }
        StringBuilder table = new StringBuilder();
        for (QueryColumn column : queryColumns) {
            String columnName = column.getName();
            table.append("| ").append(columnName).append(" ");
        }
        table.append("|\n");
        for (int i = 0; i < queryColumns.size(); i++) {
            table.append("|:---:");
        }
        table.append("|\n");
        if (queryResults == null) {
            return table.toString();
        }
        for (Map<String, Object> row : queryResults) {
            for (QueryColumn column : queryColumns) {
                String columnKey = column.getBizName();
                Object value = row.get(columnKey);
                table.append("| ").append(value != null ? value.toString() : "").append(" ");
            }
            table.append("|\n");
        }
        return table.toString();
    }
}
