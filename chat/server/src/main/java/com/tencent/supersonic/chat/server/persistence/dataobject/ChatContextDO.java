package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@TableName("s2_chat_context")
public class ChatContextDO implements Serializable {

    @TableId
    private Integer chatId;
    private Instant modifiedAt;
    @TableField("query_user")
    private String queryUser;
    private String queryText;
    private String semanticParse;
}
