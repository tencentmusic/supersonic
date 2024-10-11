package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.QueryResult;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@ToString
public class SemanticQueryResp extends QueryResult<Map<String, Object>> {

    List<QueryColumn> columns = Lists.newArrayList();
    String sql;
    QueryAuthorization queryAuthorization;
    boolean useCache;
    private String errorMsg;

    public List<QueryColumn> getMetricColumns() {
        return columns.stream()
                .filter(queryColumn -> SemanticType.NUMBER.name().equals(queryColumn.getShowType()))
                .collect(Collectors.toList());
    }

    public List<QueryColumn> getDimensionColumns() {
        return columns.stream().filter(
                queryColumn -> !SemanticType.NUMBER.name().equals(queryColumn.getShowType()))
                .collect(Collectors.toList());
    }

    public void appendErrorMsg(String msg) {
        errorMsg = StringUtil.append(errorMsg, msg);
    }
}
