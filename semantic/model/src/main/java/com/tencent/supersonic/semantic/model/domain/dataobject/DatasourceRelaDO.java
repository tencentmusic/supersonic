package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class DatasourceRelaDO {
    /**
     *
     */
    private Long id;

    /**
     *
     */
    private Long modelId;

    /**
     *
     */
    private Long datasourceFrom;

    /**
     *
     */
    private Long datasourceTo;

    /**
     *
     */
    private String joinKey;

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
     * @return model_id
     */
    public Long getModelId() {
        return modelId;
    }

    /**
     * @param modelId
     */
    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    /**
     * @return datasource_from
     */
    public Long getDatasourceFrom() {
        return datasourceFrom;
    }

    /**
     * @param datasourceFrom
     */
    public void setDatasourceFrom(Long datasourceFrom) {
        this.datasourceFrom = datasourceFrom;
    }

    /**
     * @return datasource_to
     */
    public Long getDatasourceTo() {
        return datasourceTo;
    }

    /**
     * @param datasourceTo
     */
    public void setDatasourceTo(Long datasourceTo) {
        this.datasourceTo = datasourceTo;
    }

    /**
     * @return join_key
     */
    public String getJoinKey() {
        return joinKey;
    }

    /**
     * @param joinKey
     */
    public void setJoinKey(String joinKey) {
        this.joinKey = joinKey == null ? null : joinKey.trim();
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
}
