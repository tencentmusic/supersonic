package com.tencent.supersonic.semantic.core.domain.dataobject;

import java.util.Date;

public class ViewInfoDO {

    /**
     *
     */
    private Long id;

    /**
     *
     */
    private Long domainId;

    /**
     * datasource、dimension、metric
     */
    private String type;

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
     * config detail
     */
    private String config;

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
     * @return domain_id
     */
    public Long getDomainId() {
        return domainId;
    }

    /**
     * @param domainId
     */
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    /**
     * datasource、dimension、metric
     *
     * @return type datasource、dimension、metric
     */
    public String getType() {
        return type;
    }

    /**
     * datasource、dimension、metric
     *
     * @param type datasource、dimension、metric
     */
    public void setType(String type) {
        this.type = type == null ? null : type.trim();
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
     * config detail
     *
     * @return config config detail
     */
    public String getConfig() {
        return config;
    }

    /**
     * config detail
     *
     * @param config config detail
     */
    public void setConfig(String config) {
        this.config = config == null ? null : config.trim();
    }
}