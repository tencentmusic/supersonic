package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import lombok.Data;

@Data
public class MapResp {

    private String queryText;

    private SchemaMapInfo mapInfo = new SchemaMapInfo();

}
