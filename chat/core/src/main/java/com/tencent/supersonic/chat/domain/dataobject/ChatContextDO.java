package com.tencent.supersonic.chat.domain.dataobject;

import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

@Data
public class ChatContextDO implements Serializable {

    private Integer chatId;
    private Instant modifiedAt;
    private String user;
    private String queryText;
    private String semanticParse;
}
