package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class MetricDO {
    /**
     * 
     */
    private Long id;

    /**
     * 主体域ID
     */
    private Long modelId;

    /**
     * 指标名称
     */
    private String name;

    /**
     * 字段名称
     */
    private String bizName;

    /**
     * 描述
     */
    private String description;

    /**
     * 指标状态,0正常,1下架,2删除
     */
    private Integer status;

    /**
     * 敏感级别
     */
    private Integer sensitiveLevel;

    /**
     * 指标类型 proxy,expr
     */
    private String type;

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
     * 数值类型
     */
    private String dataFormatType;

    /**
     * 数值类型参数
     */
    private String dataFormat;

    /**
     * 
     */
    private String alias;

    /**
     * 
     */
    private String tags;

    /**
     * 
     */
    private String relateDimensions;

    /**
     * 类型参数
     */
    private String typeParams;

    /**
     * 
     * @return id 
     */
    public Long getId() {
        return id;
    }

    /**
     * 
     * @param id 
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 主体域ID
     * @return model_id 主体域ID
     */
    public Long getModelId() {
        return modelId;
    }

    /**
     * 主体域ID
     * @param modelId 主体域ID
     */
    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    /**
     * 指标名称
     * @return name 指标名称
     */
    public String getName() {
        return name;
    }

    /**
     * 指标名称
     * @param name 指标名称
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 字段名称
     * @return biz_name 字段名称
     */
    public String getBizName() {
        return bizName;
    }

    /**
     * 字段名称
     * @param bizName 字段名称
     */
    public void setBizName(String bizName) {
        this.bizName = bizName == null ? null : bizName.trim();
    }

    /**
     * 描述
     * @return description 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 描述
     * @param description 描述
     */
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    /**
     * 指标状态,0正常,1下架,2删除
     * @return status 指标状态,0正常,1下架,2删除
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 指标状态,0正常,1下架,2删除
     * @param status 指标状态,0正常,1下架,2删除
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 敏感级别
     * @return sensitive_level 敏感级别
     */
    public Integer getSensitiveLevel() {
        return sensitiveLevel;
    }

    /**
     * 敏感级别
     * @param sensitiveLevel 敏感级别
     */
    public void setSensitiveLevel(Integer sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    /**
     * 指标类型 proxy,expr
     * @return type 指标类型 proxy,expr
     */
    public String getType() {
        return type;
    }

    /**
     * 指标类型 proxy,expr
     * @param type 指标类型 proxy,expr
     */
    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    /**
     * 创建时间
     * @return created_at 创建时间
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * 创建时间
     * @param createdAt 创建时间
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 创建人
     * @return created_by 创建人
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 创建人
     * @param createdBy 创建人
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy == null ? null : createdBy.trim();
    }

    /**
     * 更新时间
     * @return updated_at 更新时间
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 更新时间
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 更新人
     * @return updated_by 更新人
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 更新人
     * @param updatedBy 更新人
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy == null ? null : updatedBy.trim();
    }

    /**
     * 数值类型
     * @return data_format_type 数值类型
     */
    public String getDataFormatType() {
        return dataFormatType;
    }

    /**
     * 数值类型
     * @param dataFormatType 数值类型
     */
    public void setDataFormatType(String dataFormatType) {
        this.dataFormatType = dataFormatType == null ? null : dataFormatType.trim();
    }

    /**
     * 数值类型参数
     * @return data_format 数值类型参数
     */
    public String getDataFormat() {
        return dataFormat;
    }

    /**
     * 数值类型参数
     * @param dataFormat 数值类型参数
     */
    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat == null ? null : dataFormat.trim();
    }

    /**
     * 
     * @return alias 
     */
    public String getAlias() {
        return alias;
    }

    /**
     * 
     * @param alias 
     */
    public void setAlias(String alias) {
        this.alias = alias == null ? null : alias.trim();
    }

    /**
     * 
     * @return tags 
     */
    public String getTags() {
        return tags;
    }

    /**
     * 
     * @param tags 
     */
    public void setTags(String tags) {
        this.tags = tags == null ? null : tags.trim();
    }

    /**
     * 
     * @return relate_dimensions 
     */
    public String getRelateDimensions() {
        return relateDimensions;
    }

    /**
     * 
     * @param relateDimensions 
     */
    public void setRelateDimensions(String relateDimensions) {
        this.relateDimensions = relateDimensions == null ? null : relateDimensions.trim();
    }

    /**
     * 类型参数
     * @return type_params 类型参数
     */
    public String getTypeParams() {
        return typeParams;
    }

    /**
     * 类型参数
     * @param typeParams 类型参数
     */
    public void setTypeParams(String typeParams) {
        this.typeParams = typeParams == null ? null : typeParams.trim();
    }
}