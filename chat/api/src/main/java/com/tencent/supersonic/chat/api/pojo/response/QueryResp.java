package com.tencent.supersonic.chat.api.pojo.response;

import java.util.Date;
import java.util.List;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class QueryResp {

    private Long questionId;
    private Date createTime;
    private Long chatId;
    private Integer score;
    private String feedback;
    private String queryText;
    private QueryResult queryResult;
    private List<SemanticParseInfo> parseInfos;
}