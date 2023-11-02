package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import org.springframework.context.ApplicationEvent;

public class DataDeleteEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    private String name;
    private Long modelId;
    private Long id;
    private TypeEnums type;

    public DataDeleteEvent(Object source, String name, Long modelId, Long id, TypeEnums type) {
        super(source);
        this.name = name;
        this.modelId = modelId;
        this.id = id;
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public void setType(TypeEnums type) {
        this.type = type;
    }

    public TypeEnums getType() {
        return type;
    }
}
