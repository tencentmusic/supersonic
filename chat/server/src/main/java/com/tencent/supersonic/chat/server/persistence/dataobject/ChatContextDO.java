package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class ChatContextDO implements Serializable {

    private Integer chatId;
    private Instant modifiedAt;
    private String user;
    private String queryText;
    private String semanticParse;
}
