package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class DimensionDO {
    /**
     * 维度ID
     */
    private Long id;

    /**
     * 主题域id
     */
    private Long modelId;

    /**
     * 所属数据源id
     */
    private Long datasourceId;

    /**
     * 维度名称
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
     * 维度状态,0正常,1下架,2删除
     */
    private Integer status;

    /**
     * 敏感级别
     */
    private Integer sensitiveLevel;

    /**
     * 维度类型 categorical,time
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
     * 语义类型DATE, ID, CATEGORY
     */
    private String semanticType;

    /**
     *
     */
    private String alias;

    /**
     * default values of dimension when query
     */
    private String defaultValues;

    /**
     *
     */
    private String dimValueMaps;

    /**
     * 类型参数
     */
    private String typeParams;

    /**
     * 表达式
     */
    private String expr;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 维度ID
     *
     * @return id 维度ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 维度ID
     *
     * @param id 维度ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 主题域id
     *
     * @return model_id 主题域id
     */
    public Long getModelId() {
        return modelId;
    }

    /**
     * 主题域id
     *
     * @param modelId 主题域id
     */
    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    /**
     * 所属数据源id
     *
     * @return datasource_id 所属数据源id
     */
    public Long getDatasourceId() {
        return datasourceId;
    }

    /**
     * 所属数据源id
     *
     * @param datasourceId 所属数据源id
     */
    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    /**
     * 维度名称
     *
     * @return name 维度名称
     */
    public String getName() {
        return name;
    }

    /**
     * 维度名称
     *
     * @param name 维度名称
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 字段名称
     *
     * @return biz_name 字段名称
     */
    public String getBizName() {
        return bizName;
    }

    /**
     * 字段名称
     *
     * @param bizName 字段名称
     */
    public void setBizName(String bizName) {
        this.bizName = bizName == null ? null : bizName.trim();
    }

    /**
     * 描述
     *
     * @return description 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 描述
     *
     * @param description 描述
     */
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    /**
     * 维度状态,0正常,1下架,2删除
     *
     * @return status 维度状态,0正常,1下架,2删除
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 维度状态,0正常,1下架,2删除
     *
     * @param status 维度状态,0正常,1下架,2删除
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 敏感级别
     *
     * @return sensitive_level 敏感级别
     */
    public Integer getSensitiveLevel() {
        return sensitiveLevel;
    }

    /**
     * 敏感级别
     *
     * @param sensitiveLevel 敏感级别
     */
    public void setSensitiveLevel(Integer sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    /**
     * 维度类型 categorical,time
     *
     * @return type 维度类型 categorical,time
     */
    public String getType() {
        return type;
    }

    /**
     * 维度类型 categorical,time
     *
     * @param type 维度类型 categorical,time
     */
    public void setType(String type) {
        this.type = type == null ? null : type.trim();
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
     * 语义类型DATE, ID, CATEGORY
     *
     * @return semantic_type 语义类型DATE, ID, CATEGORY
     */
    public String getSemanticType() {
        return semanticType;
    }

    /**
     * 语义类型DATE, ID, CATEGORY
     *
     * @param semanticType 语义类型DATE, ID, CATEGORY
     */
    public void setSemanticType(String semanticType) {
        this.semanticType = semanticType == null ? null : semanticType.trim();
    }

    /**
     * @return alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias == null ? null : alias.trim();
    }

    /**
     * default values of dimension when query
     *
     * @return default_values default values of dimension when query
     */
    public String getDefaultValues() {
        return defaultValues;
    }

    /**
     * default values of dimension when query
     *
     * @param defaultValues default values of dimension when query
     */
    public void setDefaultValues(String defaultValues) {
        this.defaultValues = defaultValues == null ? null : defaultValues.trim();
    }

    /**
     * @return dim_value_maps
     */
    public String getDimValueMaps() {
        return dimValueMaps;
    }

    /**
     * @param dimValueMaps
     */
    public void setDimValueMaps(String dimValueMaps) {
        this.dimValueMaps = dimValueMaps == null ? null : dimValueMaps.trim();
    }

    /**
     * 类型参数
     *
     * @return type_params 类型参数
     */
    public String getTypeParams() {
        return typeParams;
    }

    /**
     * 类型参数
     *
     * @param typeParams 类型参数
     */
    public void setTypeParams(String typeParams) {
        this.typeParams = typeParams == null ? null : typeParams.trim();
    }

    /**
     * 表达式
     *
     * @return expr 表达式
     */
    public String getExpr() {
        return expr;
    }

    /**
     * 表达式
     *
     * @param expr 表达式
     */
    public void setExpr(String expr) {
        this.expr = expr == null ? null : expr.trim();
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}