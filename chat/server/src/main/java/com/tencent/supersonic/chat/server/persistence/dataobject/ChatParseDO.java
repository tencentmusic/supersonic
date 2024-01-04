package com.tencent.supersonic.chat.server.persistence.dataobject;

import java.util.Date;


public class ChatParseDO {

    /**
     * questionId
     */
    private Long questionId;

    /**
     * chatId
     */
    private Long chatId;

    /**
     * parseId
     */
    private Integer parseId;

    /**
     * createTime
     */
    private Date createTime;

    /**
     * queryText
     */
    private String queryText;

    /**
     * userName
     */
    private String userName;


    /**
     * parseInfo
     */
    private String parseInfo;

    /**
     * isCandidate
     */
    private Integer isCandidate;

    /**
     * return question_id
     */
    public Long getQuestionId() {
        return questionId;
    }

    /**
     * questionId
     */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    /**
     * return create_time
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * createTime
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * return user_name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * userName
     */
    public void setUserName(String userName) {
        this.userName = userName == null ? null : userName.trim();
    }

    /**
     * return chat_id
     */
    public Long getChatId() {
        return chatId;
    }

    /**
     * chatId
     */
    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    /**
     * return query_text
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * queryText
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText == null ? null : queryText.trim();
    }

    public Integer getIsCandidate() {
        return isCandidate;
    }

    public Integer getParseId() {
        return parseId;
    }

    public String getParseInfo() {
        return parseInfo;
    }

    public void setParseId(Integer parseId) {
        this.parseId = parseId;
    }

    public void setIsCandidate(Integer isCandidate) {
        this.isCandidate = isCandidate;
    }

    public void setParseInfo(String parseInfo) {
        this.parseInfo = parseInfo;
    }
}
