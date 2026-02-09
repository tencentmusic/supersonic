package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class DiscoveredSchema {
    private List<DiscoveredTable> tables;

    @Data
    public static class DiscoveredTable {
        private String tableName;
        private String tableComment;
        private List<DiscoveredColumn> columns;
    }

    @Data
    public static class DiscoveredColumn {
        private String columnName;
        private String columnType;
        private String columnComment;
        private boolean nullable;
    }
}
