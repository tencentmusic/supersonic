package com.tencent.supersonic.semantic.core.domain.dataobject;

import java.util.Date;

public class DictionaryDO {

    /**
     *
     */
    private Long id;

    /**
     * 对应维度id、指标id等
     */
    private Long itemId;

    /**
     * 对应维度、指标等
     */
    private String type;

    /**
     * 1-开启写入字典，0-不开启
     */
    private Boolean isDictInfo;

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
     * 1-删除,0-可用
     */
    private Boolean isDeleted;

    /**
     * 字典黑名单
     */
    private String blackList;

    /**
     * 字典白名单
     */
    private String whiteList;

    /**
     * 字典规则
     */
    private String ruleList;

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
     * 对应维度id、指标id等
     *
     * @return item_id 对应维度id、指标id等
     */
    public Long getItemId() {
        return itemId;
    }

    /**
     * 对应维度id、指标id等
     *
     * @param itemId 对应维度id、指标id等
     */
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    /**
     * 对应维度、指标等
     *
     * @return type 对应维度、指标等
     */
    public String getType() {
        return type;
    }

    /**
     * 对应维度、指标等
     *
     * @param type 对应维度、指标等
     */
    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    /**
     * 1-开启写入字典，0-不开启
     *
     * @return is_dict_Info 1-开启写入字典，0-不开启
     */
    public Boolean getIsDictInfo() {
        return isDictInfo;
    }

    /**
     * 1-开启写入字典，0-不开启
     *
     * @param isDictInfo 1-开启写入字典，0-不开启
     */
    public void setIsDictInfo(Boolean isDictInfo) {
        this.isDictInfo = isDictInfo;
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
     * 1-删除,0-可用
     *
     * @return is_deleted 1-删除,0-可用
     */
    public Boolean getIsDeleted() {
        return isDeleted;
    }

    /**
     * 1-删除,0-可用
     *
     * @param isDeleted 1-删除,0-可用
     */
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * 字典黑名单
     *
     * @return black_list 字典黑名单
     */
    public String getBlackList() {
        return blackList;
    }

    /**
     * 字典黑名单
     *
     * @param blackList 字典黑名单
     */
    public void setBlackList(String blackList) {
        this.blackList = blackList == null ? null : blackList.trim();
    }

    /**
     * 字典白名单
     *
     * @return white_list 字典白名单
     */
    public String getWhiteList() {
        return whiteList;
    }

    /**
     * 字典白名单
     *
     * @param whiteList 字典白名单
     */
    public void setWhiteList(String whiteList) {
        this.whiteList = whiteList == null ? null : whiteList.trim();
    }

    /**
     * 字典规则
     *
     * @return rule_list 字典规则
     */
    public String getRuleList() {
        return ruleList;
    }

    /**
     * 字典规则
     *
     * @param ruleList 字典规则
     */
    public void setRuleList(String ruleList) {
        this.ruleList = ruleList == null ? null : ruleList.trim();
    }
}