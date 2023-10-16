package com.tencent.supersonic.semantic.materialization.domain.dataobject;

public class MaterializationElementDOWithBLOBs extends MaterializationElementDO {
    private String depends;

    private String description;

    public String getDepends() {
        return depends;
    }

    public void setDepends(String depends) {
        this.depends = depends == null ? null : depends.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}