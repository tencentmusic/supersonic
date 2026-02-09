package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SchemaChange {
    private SchemaChangeStatus status;
    private List<StreamChange> changes;

    @Data
    public static class StreamChange {
        private String streamName;
        private ChangeType changeType;
        private List<ColumnChange> columnChanges;
    }

    @Data
    public static class ColumnChange {
        private String columnName;
        private ChangeType changeType;
        private String previousType;
        private String currentType;
    }

    public enum ChangeType {
        ADDED, REMOVED, TYPE_CHANGED, NULLABLE_CHANGED, PRIMARY_KEY_CHANGED
    }
}
