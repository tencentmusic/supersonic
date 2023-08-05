package com.tencent.supersonic.chat.api.pojo.response;

import java.util.Date;
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
}