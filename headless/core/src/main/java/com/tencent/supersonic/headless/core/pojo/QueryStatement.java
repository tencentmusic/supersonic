package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Ontology;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.OntologyQueryParam;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

@Data
public class QueryStatement {

    private Long dataSetId;
    private String sql;
    private String errMsg;
    private StructQueryParam structQueryParam;
    private SqlQueryParam sqlQueryParam;
    private OntologyQueryParam ontologyQueryParam;
    private Integer status = 0;
    private Boolean isS2SQL = false;
    private Boolean enableOptimize = true;
    private Triple<String, String, String> minMaxTime;
    private Ontology ontology;
    private SemanticSchemaResp semanticSchemaResp;
    private Integer limit = 1000;
    private Boolean isTranslated = false;

    public boolean isOk() {
        return StringUtils.isBlank(errMsg) && StringUtils.isNotBlank(sql);
    }

    public boolean isTranslated() {
        return isTranslated != null && isTranslated && isOk();
    }
}
