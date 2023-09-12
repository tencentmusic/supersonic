package com.tencent.supersonic.chat.persistence.dataobject;

import java.util.Date;

public class AgentDO {
    /**
     */
    private Integer id;

    /**
     */
    private String name;

    /**
     */
    private String description;

    /**
     * 0 offline, 1 online
     */
    private Integer status;

    /**
     */
    private String examples;

    /**
     */
    private String config;

    /**
     */
    private String createdBy;

    /**
     */
    private Date createdAt;

    /**
     */
    private String updatedBy;

    /**
     */
    private Date updatedAt;

    /**
     */
    private Integer enableSearch;

    /**
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
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
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    /**
     * 0 offline, 1 online
     * @return status 0 offline, 1 online
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 0 offline, 1 online
     * @param status 0 offline, 1 online
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * @return examples
     */
    public String getExamples() {
        return examples;
    }

    /**
     * @param examples
     */
    public void setExamples(String examples) {
        this.examples = examples == null ? null : examples.trim();
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
     * @return enable_search
     */
    public Integer getEnableSearch() {
        return enableSearch;
    }

    /**
     * @param enableSearch
     */
    public void setEnableSearch(Integer enableSearch) {
        this.enableSearch = enableSearch;
    }
}
