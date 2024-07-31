package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import lombok.Data;

@Data
public class MapResp {

    private final String queryText;

    private SchemaMapInfo mapInfo = new SchemaMapInfo();

    public MapResp(String queryText) {
        this.queryText = queryText;
    }
}
