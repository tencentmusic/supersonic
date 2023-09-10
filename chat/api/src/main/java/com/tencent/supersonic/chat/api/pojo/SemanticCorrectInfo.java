package com.tencent.supersonic.chat.api.pojo;

import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SemanticCorrectInfo {

    private QueryFilters queryFilters;

    private SemanticParseInfo parseInfo;

    private String sql;

    private String preSql;
}
