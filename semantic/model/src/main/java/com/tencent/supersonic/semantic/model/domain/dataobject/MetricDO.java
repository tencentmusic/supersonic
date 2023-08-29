package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class MetricDO {

    private Long id;

    private Long modelId;

    private String name;

    private String bizName;

    private String description;

    private Integer status;

    private Integer sensitiveLevel;

    private String type;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String dataFormatType;

    private String dataFormat;

    private String alias;

    private String typeParams;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getBizName() {
        return bizName;
    }

    public void setBizName(String bizName) {
        this.bizName = bizName == null ? null : bizName.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(Integer sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

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
