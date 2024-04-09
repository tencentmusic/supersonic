package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class QueryReq {
    private String queryText;
    private Integer chatId;
    private Set<Long> dataSetIds = Sets.newHashSet();
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private Text2SQLType text2SQLType = Text2SQLType.RULE_AND_LLM;
    private MapModeEnum mapModeEnum = MapModeEnum.STRICT;
    private List<SemanticParseInfo> contextualParseInfoList;
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
}
