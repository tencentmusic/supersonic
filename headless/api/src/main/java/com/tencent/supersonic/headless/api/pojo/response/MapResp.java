package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import lombok.Data;

@Data
public class MapResp {

    private final String queryText;
    private final SchemaMapInfo mapInfo;

    public MapResp(String queryText, SchemaMapInfo schemaMapInfo) {
        this.queryText = queryText;
        this.mapInfo = schemaMapInfo;
    }
}
