package com.tencent.supersonic.chat.domain.dataobject;

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
    public String aggregator = "trend";
    private int topNum;
    public String startTime;
    public String endTime;
    private String querySql;
    private Object queryColumn;
    private Object entityInfo;
    private int score;
    private String feedback;
}
