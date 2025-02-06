package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.headless.api.pojo.QueryDataType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class QueryNLReq extends SemanticQueryReq implements Serializable {
    private Long queryId;
    private String queryText;
    private Set<Long> dataSetIds = Sets.newHashSet();
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private Text2SQLType text2SQLType = Text2SQLType.LLM_OR_RULE;
    private MapModeEnum mapModeEnum = MapModeEnum.STRICT;
    private QueryDataType queryDataType = QueryDataType.ALL;
    private Map<String, ChatApp> chatAppConfig;
    private List<Text2SQLExemplar> dynamicExemplars = Lists.newArrayList();
    private SemanticParseInfo contextParseInfo;
    private SemanticParseInfo selectedParseInfo;
    private boolean descriptionMapped;

    @Override
    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"queryText\":").append(dataSetId);
        stringBuilder.append("\"dataSetId\":").append(dataSetId);
        stringBuilder.append("\"modelIds\":").append(modelIds);
        stringBuilder.append(",\"params\":").append(params);
        stringBuilder.append(",\"cacheInfo\":").append(cacheInfo);
        stringBuilder.append(",\"mapMode\":").append(mapModeEnum);
        stringBuilder.append(",\"dataType\":").append(queryDataType);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
