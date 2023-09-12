package com.tencent.supersonic.semantic.model.domain.dataobject;

import java.util.Date;

public class ModelDO {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private String bizName;

    /**
     * 
     */
    private Long domainId;

    /**
     * 
     */
    private String alias;

    /**
     * 
     */
    private String viewer;

    /**
     * 
     */
    private String viewOrg;

    /**
     * 
     */
    private String admin;

    /**
     * 
     */
    private String adminOrg;

    /**
     * 
     */
    private Integer isOpen;

    /**
     * 
     */
    private String createdBy;

    /**
     * 
     */
    private Date createdAt;

    /**
     * 
     */
    private String updatedBy;

    /**
     * 
     */
    private Date updatedAt;

    /**
     * 
     */
    private String entity;

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
     * 
     * @return name 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name 
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 
     * @return biz_name 
     */
    public String getBizName() {
        return bizName;
    }

    /**
     * 
     * @param bizName 
     */
    public void setBizName(String bizName) {
        this.bizName = bizName == null ? null : bizName.trim();
    }

    /**
     * 
     * @return domain_id 
     */
    public Long getDomainId() {
        return domainId;
    }

    /**
     * 
     * @param domainId 
     */
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
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
     * @return viewer 
     */
    public String getViewer() {
        return viewer;
    }

    /**
     * 
     * @param viewer 
     */
    public void setViewer(String viewer) {
        this.viewer = viewer == null ? null : viewer.trim();
    }

    /**
     * 
     * @return view_org 
     */
    public String getViewOrg() {
        return viewOrg;
    }

    /**
     * 
     * @param viewOrg 
     */
    public void setViewOrg(String viewOrg) {
        this.viewOrg = viewOrg == null ? null : viewOrg.trim();
    }

    /**
     * 
     * @return admin 
     */
    public String getAdmin() {
        return admin;
    }

    /**
     * 
     * @param admin 
     */
    public void setAdmin(String admin) {
        this.admin = admin == null ? null : admin.trim();
    }

    /**
     * 
     * @return admin_org 
     */
    public String getAdminOrg() {
        return adminOrg;
    }

    /**
     * 
     * @param adminOrg 
     */
    public void setAdminOrg(String adminOrg) {
        this.adminOrg = adminOrg == null ? null : adminOrg.trim();
    }

    /**
     * 
     * @return is_open 
     */
    public Integer getIsOpen() {
        return isOpen;
    }

    /**
     * 
     * @param isOpen 
     */
    public void setIsOpen(Integer isOpen) {
        this.isOpen = isOpen;
    }

    /**
     * 
     * @return created_by 
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 
     * @param createdBy 
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy == null ? null : createdBy.trim();
    }

    /**
     * 
     * @return created_at 
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * 
     * @param createdAt 
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 
     * @return updated_by 
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * 
     * @param updatedBy 
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy == null ? null : updatedBy.trim();
    }

    /**
     * 
     * @return updated_at 
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 
     * @param updatedAt 
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 
     * @return entity 
     */
    public String getEntity() {
        return entity;
    }

    /**
     * 
     * @param entity 
     */
    public void setEntity(String entity) {
        this.entity = entity == null ? null : entity.trim();
    }
}