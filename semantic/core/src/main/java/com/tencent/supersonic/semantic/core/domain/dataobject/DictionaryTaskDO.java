package com.tencent.supersonic.semantic.core.domain.dataobject;

public class DictionaryTaskDO {

    /**
     *
     */
    private Long id;

    /**
     * 主体域ID
     */
    private Long domainId;

    /**
     * 任务最终运行状态
     */
    private Integer status;

    /**
     * 任务耗时
     */
    private Long elapsedMs;

    /**
     * 查询涉及的维度
     */
    private String dimensions;

    /**
     * 查询涉及的指标
     */
    private String metrics;

    /**
     * 查询的过滤信息
     */
    private String filters;

    /**
     * 查询的排序信息
     */
    private String orderBy;

    /**
     * 查询涉及的日期信息
     */
    private String dateInfo;

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
     * 主体域ID
     *
     * @return domain_id 主体域ID
     */
    public Long getDomainId() {
        return domainId;
    }

    /**
     * 主体域ID
     *
     * @param domainId 主体域ID
     */
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    /**
     * 任务最终运行状态
     *
     * @return status 任务最终运行状态
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 任务最终运行状态
     *
     * @param status 任务最终运行状态
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 任务耗时
     *
     * @return elapsed_ms 任务耗时
     */
    public Long getElapsedMs() {
        return elapsedMs;
    }

    /**
     * 任务耗时
     *
     * @param elapsedMs 任务耗时
     */
    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    /**
     * 查询涉及的维度
     *
     * @return dimensions 查询涉及的维度
     */
    public String getDimensions() {
        return dimensions;
    }

    /**
     * 查询涉及的维度
     *
     * @param dimensions 查询涉及的维度
     */
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions == null ? null : dimensions.trim();
    }

    /**
     * 查询涉及的指标
     *
     * @return metrics 查询涉及的指标
     */
    public String getMetrics() {
        return metrics;
    }

    /**
     * 查询涉及的指标
     *
     * @param metrics 查询涉及的指标
     */
    public void setMetrics(String metrics) {
        this.metrics = metrics == null ? null : metrics.trim();
    }

    /**
     * 查询的过滤信息
     *
     * @return filters 查询的过滤信息
     */
    public String getFilters() {
        return filters;
    }

    /**
     * 查询的过滤信息
     *
     * @param filters 查询的过滤信息
     */
    public void setFilters(String filters) {
        this.filters = filters == null ? null : filters.trim();
    }

    /**
     * 查询的排序信息
     *
     * @return order_by 查询的排序信息
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * 查询的排序信息
     *
     * @param orderBy 查询的排序信息
     */
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy == null ? null : orderBy.trim();
    }

    /**
     * 查询涉及的日期信息
     *
     * @return date_info 查询涉及的日期信息
     */
    public String getDateInfo() {
        return dateInfo;
    }

    /**
     * 查询涉及的日期信息
     *
     * @param dateInfo 查询涉及的日期信息
     */
    public void setDateInfo(String dateInfo) {
        this.dateInfo = dateInfo == null ? null : dateInfo.trim();
    }
}