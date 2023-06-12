package com.tencent.supersonic.chat.domain.pojo.chat;

import com.tencent.supersonic.chat.api.response.QueryResultResp;
import java.util.Date;
import lombok.Data;

@Data
public class ChatQueryVO {

    private Long questionId;
    private Date createTime;
    private Long chatId;
    private Integer score;
    private String feedback;
    private String queryText;
    private QueryResultResp queryResponse;
}