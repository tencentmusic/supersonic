package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class DomainDO {
    /**
     * 自增ID
     */
    private Long id;

    /**
     * 主题域名称
     */
    private String name;

    /**
     * 内部名称
     */
    private String bizName;

    /**
     * 父主题域ID
     */
    private Long parentId;

    /**
     * 主题域状态
     */
    private Integer status;

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
     * 主题域管理员
     */
    private String admin;

    /**
     * 主题域管理员组织
     */
    private String adminOrg;

    /**
     * 主题域是否公开
     */
    private Integer isOpen;

    /**
     * 主题域可用用户
     */
    private String viewer;

    /**
     * 主题域可用组织
     */
    private String viewOrg;

    /**
     * 自增ID
     * @return id 自增ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 自增ID
     * @param id 自增ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 主题域名称
     * @return name 主题域名称
     */
    public String getName() {
        return name;
    }

    /**
     * 主题域名称
     * @param name 主题域名称
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 内部名称
     * @return biz_name 内部名称
     */
    public String getBizName() {
        return bizName;
    }

    /**
     * 内部名称
     * @param bizName 内部名称
     */
    public void setBizName(String bizName) {
        this.bizName = bizName == null ? null : bizName.trim();
    }

    /**
     * 父主题域ID
     * @return parent_id 父主题域ID
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 父主题域ID
     * @param parentId 父主题域ID
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 主题域状态
     * @return status 主题域状态
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 主题域状态
     * @param status 主题域状态
     */
    public void setStatus(Integer status) {
        this.status = status;
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
     * 主题域管理员
     * @return admin 主题域管理员
     */
    public String getAdmin() {
        return admin;
    }

    /**
     * 主题域管理员
     * @param admin 主题域管理员
     */
    public void setAdmin(String admin) {
        this.admin = admin == null ? null : admin.trim();
    }

    /**
     * 主题域管理员组织
     * @return admin_org 主题域管理员组织
     */
    public String getAdminOrg() {
        return adminOrg;
    }

    /**
     * 主题域管理员组织
     * @param adminOrg 主题域管理员组织
     */
    public void setAdminOrg(String adminOrg) {
        this.adminOrg = adminOrg == null ? null : adminOrg.trim();
    }

    /**
     * 主题域是否公开
     * @return is_open 主题域是否公开
     */
    public Integer getIsOpen() {
        return isOpen;
    }

    /**
     * 主题域是否公开
     * @param isOpen 主题域是否公开
     */
    public void setIsOpen(Integer isOpen) {
        this.isOpen = isOpen;
    }

    /**
     * 主题域可用用户
     * @return viewer 主题域可用用户
     */
    public String getViewer() {
        return viewer;
    }

    /**
     * 主题域可用用户
     * @param viewer 主题域可用用户
     */
    public void setViewer(String viewer) {
        this.viewer = viewer == null ? null : viewer.trim();
    }

    /**
     * 主题域可用组织
     * @return view_org 主题域可用组织
     */
    public String getViewOrg() {
        return viewOrg;
    }

    /**
     * 主题域可用组织
     * @param viewOrg 主题域可用组织
     */
    public void setViewOrg(String viewOrg) {
        this.viewOrg = viewOrg == null ? null : viewOrg.trim();
    }
}