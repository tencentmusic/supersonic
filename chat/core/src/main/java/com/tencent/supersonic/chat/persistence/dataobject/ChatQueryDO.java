package com.tencent.supersonic.chat.persistence.dataobject;

import java.util.Date;


public class ChatQueryDO {

    /**
     * questionId
     */
    private Long questionId;

    /**
     * createTime
     */
    private Date createTime;

    /**
     * userName
     */
    private String userName;

    /**
     * queryState
     */
    private Integer queryState;

    /**
     * chatId
     */
    private Long chatId;

    /**
     * score
     */
    private Integer score;

    /**
     * feedback
     */
    private String feedback;

    /**
     * queryText
     */
    private String queryText;

    /**
     * queryResponse
     */
    private String queryResult;

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
     * return query_state
     */
    public Integer getQueryState() {
        return queryState;
    }

    /**
     * queryState
     */
    public void setQueryState(Integer queryState) {
        this.queryState = queryState;
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
     * return score
     */
    public Integer getScore() {
        return score;
    }

    /**
     * score
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * return feedback
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     * feedback
     */
    public void setFeedback(String feedback) {
        this.feedback = feedback == null ? null : feedback.trim();
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

    /**
     * return query_response
     */
    public String getQueryResult() {
        return queryResult;
    }

    /**
     * queryResponse
     */
    public void setQueryResult(String queryResult) {
        this.queryResult = queryResult == null ? null : queryResult.trim();
    }
}