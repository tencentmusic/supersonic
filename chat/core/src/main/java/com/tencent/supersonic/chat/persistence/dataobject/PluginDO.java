package com.tencent.supersonic.chat.persistence.dataobject;

import java.util.Date;

public class PluginDO {
    /**
     *
     */
    private Long id;

    /**
     * DASHBOARD,WIDGET,URL
     */
    private String type;

    /**
     *
     */
    private String model;

    /**
     *
     */
    private String pattern;

    /**
     *
     */
    private String parseMode;

    /**
     *
     */
    private String name;

    /**
     *
     */
    private Date createdAt;

    /**
     *
     */
    private String createdBy;

    /**
     *
     */
    private Date updatedAt;

    /**
     *
     */
    private String updatedBy;

    /**
     *
     */
    private String parseModeConfig;

    /**
     *
     */
    private String config;

    /**
     *
     */
    private String comment;

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * DASHBOARD,WIDGET,URL
     *
     * @return type DASHBOARD,WIDGET,URL
     */
    public String getType() {
        return type;
    }

    /**
     * DASHBOARD,WIDGET,URL
     *
     * @param type DASHBOARD,WIDGET,URL
     */
    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    /**
     * @return model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model
     */
    public void setModel(String model) {
        this.model = model == null ? null : model.trim();
    }

    /**
     * @return pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern == null ? null : pattern.trim();
    }

    /**
     * @return parse_mode
     */
    public String getParseMode() {
        return parseMode;
    }

    /**
     * @param parseMode
     */
    public void setParseMode(String parseMode) {
        this.parseMode = parseMode == null ? null : parseMode.trim();
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * @return created_at
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @return created_by
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy == null ? null : createdBy.trim();
    }

    /**
     * @return updated_at
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * @param updatedAt
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * @return updated_by
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * @param updatedBy
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy == null ? null : updatedBy.trim();
    }

    /**
     * @return parse_mode_config
     */
    public String getParseModeConfig() {
        return parseModeConfig;
    }

    /**
     * @param parseModeConfig
     */
    public void setParseModeConfig(String parseModeConfig) {
        this.parseModeConfig = parseModeConfig == null ? null : parseModeConfig.trim();
    }

    /**
     * @return config
     */
    public String getConfig() {
        return config;
    }

    /**
     * @param config
     */
    public void setConfig(String config) {
        this.config = config == null ? null : config.trim();
    }

    /**
     * @return comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment
     */
    public void setComment(String comment) {
        this.comment = comment == null ? null : comment.trim();
    }
}
