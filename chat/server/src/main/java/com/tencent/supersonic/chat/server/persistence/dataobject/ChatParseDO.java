package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.Data;

import java.util.Date;

@Data
public class ChatParseDO {

    private Long questionId;

    private Integer chatId;

    private Integer parseId;

    private Date createTime;

    private String queryText;

    private String userName;

    private String parseInfo;

    private Integer isCandidate;
}
