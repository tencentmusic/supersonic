package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class DatasourceDO {
    /**
     *
     */
    private Long id;

    /**
     * 主题域ID
     */
    private Long modelId;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 内部名称
     */
    private String bizName;

    /**
     * 数据源描述
     */
    private String description;

    /**
     * 数据库实例ID
     */
    private Long databaseId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 数据源配置
     */
    private String datasourceDetail;

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
     * 主题域ID
     *
     * @return model_id 主题域ID
     */
    public Long getModelId() {
        return modelId;
    }

    /**
     * 主题域ID
     *
     * @param modelId 主题域ID
     */
    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    /**
     * 数据源名称
     *
     * @return name 数据源名称
     */
    public String getName() {
        return name;
    }

    /**
     * 数据源名称
     *
     * @param name 数据源名称
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 内部名称
     *
     * @return biz_name 内部名称
     */
    public String getBizName() {
        return bizName;
    }

    /**
     * 内部名称
     *
     * @param bizName 内部名称
     */
    public void setBizName(String bizName) {
        this.bizName = bizName == null ? null : bizName.trim();
    }

    /**
     * 数据源描述
     *
     * @return description 数据源描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 数据源描述
     *
     * @param description 数据源描述
     */
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    /**
     * 数据库实例ID
     *
     * @return database_id 数据库实例ID
     */
    public Long getDatabaseId() {
        return databaseId;
    }

    /**
     * 数据库实例ID
     *
     * @param databaseId 数据库实例ID
     */
    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    /**
     * 创建时间
     *
     * @return created_at 创建时间
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * 创建时间
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 创建人
     *
     * @return created_by 创建人
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 创建人
     *
     * @param createdBy 创建人
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy == null ? null : createdBy.trim();
    }

    /**
     * 更新时间
     *
     * @return updated_at 更新时间
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 更新时间
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 更新人
     *
     * @return updated_by 更新人
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 更新人
     *
     * @param updatedBy 更新人
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy == null ? null : updatedBy.trim();
    }

    /**
     * 数据源配置
     *
     * @return datasource_detail 数据源配置
     */
    public String getDatasourceDetail() {
        return datasourceDetail;
    }

    /**
     * 数据源配置
     *
     * @param datasourceDetail 数据源配置
     */
    public void setDatasourceDetail(String datasourceDetail) {
        this.datasourceDetail = datasourceDetail == null ? null : datasourceDetail.trim();
    }
}
