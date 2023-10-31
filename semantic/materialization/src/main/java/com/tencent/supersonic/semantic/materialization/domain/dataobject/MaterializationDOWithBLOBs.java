package com.tencent.supersonic.semantic.materialization.domain.dataobject;

public class MaterializationDOWithBLOBs extends MaterializationDO {
    private String dateInfo;

    private String entities;

    private String description;

    public String getDateInfo() {
        return dateInfo;
    }

    public void setDateInfo(String dateInfo) {
        this.dateInfo = dateInfo == null ? null : dateInfo.trim();
    }

    public String getEntities() {
        return entities;
    }

    public void setEntities(String entities) {
        this.entities = entities == null ? null : entities.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}