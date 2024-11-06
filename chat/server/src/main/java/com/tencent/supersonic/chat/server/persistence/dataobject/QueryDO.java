package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.Data;

@Data
public class QueryDO {

    private long id;
    private long questionId;
    private String createTime;
    private String time;
    private String userName;
    private String question;
    private Object queryResults;
    private int state;
    private String dataContent;
    private String name;
    private int queryType;
    private int isDeleted;
    private String module;
    private long chatId;
    private int topNum;
    private String querySql;
    private Object queryColumn;
    private int score;
    private String feedback;
}
