package com.tencent.supersonic.semantic.materialization.domain.dataobject;

public class MaterializationElementDOKey {
    private Long id;

    private String type;

    private Long materializationId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    public Long getMaterializationId() {
        return materializationId;
    }

    public void setMaterializationId(Long materializationId) {
        this.materializationId = materializationId;
    }
}