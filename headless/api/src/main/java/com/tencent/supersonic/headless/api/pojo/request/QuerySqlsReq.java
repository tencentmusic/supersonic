package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class QuerySqlsReq extends SemanticQueryReq {
    private List<String> sqls;

    @Override
    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"dataSetId\":").append(dataSetId);
        stringBuilder.append("\"modelIds\":").append(modelIds);
        stringBuilder.append(",\"params\":").append(params);
        stringBuilder.append(",\"cacheInfo\":").append(cacheInfo);
        stringBuilder.append(",\"sqls\":").append(sqls);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
