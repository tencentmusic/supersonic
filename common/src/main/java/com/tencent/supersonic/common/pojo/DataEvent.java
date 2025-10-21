package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.EventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class DataEvent extends ApplicationEvent {

    private final List<DataItem> dataItems;

    private final EventType eventType;

    private final String userName;

    public DataEvent(Object source, List<DataItem> dataItems, EventType eventType,
            String userName) {
        super(source);
        this.dataItems = dataItems;
        this.eventType = eventType;
        this.userName = userName;
    }

}
