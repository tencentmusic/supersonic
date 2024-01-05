package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.Data;
import java.util.Date;

@Data
public class ChatQueryDO {
    /**
     */
    private Long questionId;

    /**
     */
    private Integer agentId;

    /**
     */
    private Date createTime;

    /**
     */
    private String userName;

    /**
     */
    private Integer queryState;

    /**
     */
    private Long chatId;

    /**
     */
    private Integer score;

    /**
     */
    private String feedback;

    /**
     */
    private String queryText;

    /**
     */
    private String queryResult;

    private String similarQueries;

}
