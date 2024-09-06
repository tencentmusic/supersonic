package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.EventType;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class DataEvent extends ApplicationEvent {

    private List<DataItem> dataItems;

    private EventType eventType;

    public DataEvent(Object source, List<DataItem> dataItems, EventType eventType) {
        super(source);
        this.dataItems = dataItems;
        this.eventType = eventType;
    }

    public List<DataItem> getDataItems() {
        return dataItems;
    }

    public EventType getEventType() {
        return eventType;
    }
}
