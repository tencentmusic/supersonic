package com.tencent.supersonic.common.pojo;

import org.springframework.context.ApplicationEvent;

public class DataUpdateEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    private String name;
    private String newName;
    private Long modelId;
    private Long id;
    private String type;

    public DataUpdateEvent(Object source, String name, String newName, Long modelId, Long id, String type) {
        super(source);
        this.name = name;
        this.newName = newName;
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

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
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

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
