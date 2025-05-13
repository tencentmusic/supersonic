package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_chat")
public class ChatDO {

    @TableId(type = IdType.AUTO)
    private Long chatId;
    private Integer agentId;
    private String chatName;
    private String createTime;
    private String lastTime;
    private String creator;
    private String lastQuestion;
    private int isDelete;
    private int isTop;
}
