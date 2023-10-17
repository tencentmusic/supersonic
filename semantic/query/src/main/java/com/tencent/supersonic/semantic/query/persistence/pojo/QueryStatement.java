package com.tencent.supersonic.semantic.query.persistence.pojo;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import java.util.List;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;

@Data
public class QueryStatement {

    private Long modelId = 0L;
    private String sql = "";
    private String sourceId = "";
    private String errMsg = "";
    private Boolean ok;
    private MetricReq metricReq;
    private ParseSqlReq parseSqlReq;
    private Integer status = 0;
    private List<ImmutablePair<String, String>> timeRanges;


    public boolean isOk() {
        this.ok = "".equals(errMsg) && !"".equals(sql);
        return ok;
    }

    public QueryStatement error(String msg) {
        this.setErrMsg(msg);
        return this;
    }
}
