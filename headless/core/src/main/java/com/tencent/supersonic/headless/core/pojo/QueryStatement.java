package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

@Data
public class QueryStatement {
    private Long dataSetId;
    private List<Long> modelIds;
    private String sql = "";
    private String sourceId = "";
    private String errMsg = "";
    private Boolean ok;
    private QueryParam queryParam;
    private MetricQueryParam metricQueryParam;
    private DataSetQueryParam dataSetQueryParam;
    private Integer status = 0;
    private Boolean isS2SQL = false;
    private List<ImmutablePair<String, String>> timeRanges;
    private Boolean enableOptimize = true;
    private Triple<String, String, String> minMaxTime;
    private String dataSetSql = "";
    private String dataSetAlias = "";
    private String dataSetSimplifySql = "";
    private Boolean enableLimitWrapper = false;

    private SemanticModel semanticModel;

    private SemanticSchemaResp semanticSchemaResp;
    private Integer limit = 1000;

    public boolean isOk() {
        this.ok = "".equals(errMsg) && !"".equals(sql);
        return ok;
    }

    public QueryStatement error(String msg) {
        this.setErrMsg(msg);
        return this;
    }
}
