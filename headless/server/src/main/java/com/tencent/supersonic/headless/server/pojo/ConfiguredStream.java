package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ConfiguredStream {
    private String streamName;
    private String namespace;
    private SyncMode syncMode;
    private String destinationSyncMode;
    private String cursorField;
    private List<String> primaryKey;
    private Boolean selected;
    private StreamSchema schema;

    @Data
    public static class StreamSchema {
        private String type;
        private List<StreamProperty> properties;
    }

    @Data
    public static class StreamProperty {
        private String name;
        private String type;
        private Boolean nullable;
    }
}
