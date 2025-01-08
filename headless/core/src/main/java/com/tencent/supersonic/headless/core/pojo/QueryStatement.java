package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

@Data
public class QueryStatement {

    private Long dataSetId;
    private String dataSetName;
    private String sql;
    private String errMsg;
    private StructQuery structQuery;
    private SqlQuery sqlQuery;
    private OntologyQuery ontologyQuery;
    private QueryState status = QueryState.SUCCESS;
    private Boolean isS2SQL = false;
    private Boolean enableOptimize = true;
    private Triple<String, String, String> minMaxTime;
    private Ontology ontology;
    private SemanticSchemaResp semanticSchema;
    private Integer limit = 1000;
    private Boolean isTranslated = false;

    public boolean isOk() {
        return StringUtils.isBlank(errMsg) && StringUtils.isNotBlank(sql);
    }

    public boolean isTranslated() {
        return isTranslated != null && isTranslated && isOk();
    }
}
