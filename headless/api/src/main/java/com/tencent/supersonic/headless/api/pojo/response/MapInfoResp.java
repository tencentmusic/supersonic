package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.Term;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MapInfoResp {

    private String queryText;

    private Map<String, List<SchemaElementMatch>> mapFields;

    private Map<String, List<SchemaElementMatch>> topFields;

    private Map<String, List<Term>> terms;

}
