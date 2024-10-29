package com.tencent.supersonic.headless.api.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;

import java.util.Objects;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuerySqlReq extends SemanticQueryReq {

    private String sql;
    private Integer limit = 1000;

    @Override
    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"dataSetId\":").append(dataSetId);
        stringBuilder.append("\"modelIds\":").append(modelIds);
        stringBuilder.append(",\"params\":").append(params);
        stringBuilder.append(",\"cacheInfo\":").append(cacheInfo);
        stringBuilder.append(",\"sql\":").append(sql);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public boolean needGetDataSetId() {
        return (Objects.isNull(this.getDataSetId()) || this.getDataSetId() <= 0)
                && (CollectionUtils.isEmpty(this.getModelIds()));
    }
}
