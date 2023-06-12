package com.tencent.supersonic.semantic.core.domain.dataobject;

import java.util.Date;

public class DomainExtendDO {

    /**
     *
     */
    private Long id;

    /**
     * 主题域id
     */
    private Long domainId;

    /**
     * 默认指标
     */
    private String defaultMetrics;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 主题域扩展信息状态, 0-删除，1-生效
     */
    private Integer status;

    /**
     * 不可见指标信息
     */
    private String metricsInvisible;

    /**
     * 不可见维度信息
     */
    private String dimensionsInvisible;

    /**
     * 实体信息
     */
    private String entityInfo;

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
     * 主题域id
     *
     * @return domain_id 主题域id
     */
    public Long getDomainId() {
        return domainId;
    }

    /**
     * 主题域id
     *
     * @param domainId 主题域id
     */
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    /**
     * 默认指标
     *
     * @return default_metrics 默认指标
     */
    public String getDefaultMetrics() {
        return defaultMetrics;
    }

    /**
     * 默认指标
     *
     * @param defaultMetrics 默认指标
     */
    public void setDefaultMetrics(String defaultMetrics) {
        this.defaultMetrics = defaultMetrics == null ? null : defaultMetrics.trim();
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
     * 主题域扩展信息状态, 0-删除，1-生效
     *
     * @return status 主题域扩展信息状态, 0-删除，1-生效
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 主题域扩展信息状态, 0-删除，1-生效
     *
     * @param status 主题域扩展信息状态, 0-删除，1-生效
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 不可见指标信息
     *
     * @return metrics_invisible 不可见指标信息
     */
    public String getMetricsInvisible() {
        return metricsInvisible;
    }

    /**
     * 不可见指标信息
     *
     * @param metricsInvisible 不可见指标信息
     */
    public void setMetricsInvisible(String metricsInvisible) {
        this.metricsInvisible = metricsInvisible == null ? null : metricsInvisible.trim();
    }

    /**
     * 不可见维度信息
     *
     * @return dimensions_invisible 不可见维度信息
     */
    public String getDimensionsInvisible() {
        return dimensionsInvisible;
    }

    /**
     * 不可见维度信息
     *
     * @param dimensionsInvisible 不可见维度信息
     */
    public void setDimensionsInvisible(String dimensionsInvisible) {
        this.dimensionsInvisible = dimensionsInvisible == null ? null : dimensionsInvisible.trim();
    }

    /**
     * 实体信息
     *
     * @return entity_info 实体信息
     */
    public String getEntityInfo() {
        return entityInfo;
    }

    /**
     * 实体信息
     *
     * @param entityInfo 实体信息
     */
    public void setEntityInfo(String entityInfo) {
        this.entityInfo = entityInfo == null ? null : entityInfo.trim();
    }
}