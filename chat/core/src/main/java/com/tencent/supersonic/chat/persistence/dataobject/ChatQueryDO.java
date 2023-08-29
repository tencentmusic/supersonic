package com.tencent.supersonic.chat.persistence.dataobject;

import java.util.Date;

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

    /**
     * @return question_id
     */
    public Long getQuestionId() {
        return questionId;
    }

    /**
     * @param questionId
     */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    /**
     * @return agent_id
     */
    public Integer getAgentId() {
        return agentId;
    }

    /**
     * @param agentId
     */
    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    /**
     * @return create_time
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * @param createTime
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * @return user_name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName == null ? null : userName.trim();
    }

    /**
     *
     * @return query_state
     */
    public Integer getQueryState() {
        return queryState;
    }

    /**
     *
     * @param queryState
     */
    public void setQueryState(Integer queryState) {
        this.queryState = queryState;
    }

    /**
     *
     * @return chat_id
     */
    public Long getChatId() {
        return chatId;
    }

    /**
     *
     * @param chatId
     */
    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    /**
     *
     * @return score
     */
    public Integer getScore() {
        return score;
    }

    /**
     *
     * @param score
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     *
     * @return feedback
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     *
     * @param feedback
     */
    public void setFeedback(String feedback) {
        this.feedback = feedback == null ? null : feedback.trim();
    }

    /**
     *
     * @return query_text
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     *
     * @param queryText
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText == null ? null : queryText.trim();
    }

    /**
     *
     * @return query_result
     */
    public String getQueryResult() {
        return queryResult;
    }

    /**
     *
     * @param queryResult
     */
    public void setQueryResult(String queryResult) {
        this.queryResult = queryResult == null ? null : queryResult.trim();
    }
}
