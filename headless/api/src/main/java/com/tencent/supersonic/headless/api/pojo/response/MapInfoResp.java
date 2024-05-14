package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.Term;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MapInfoResp {

    private String queryText;

    private Map<String, DataSetMapInfo> dataSetMapInfo = new HashMap<>();

    private Map<String, List<Term>> terms = new HashMap<>();

}
