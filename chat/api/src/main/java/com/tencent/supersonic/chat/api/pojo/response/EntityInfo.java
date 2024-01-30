package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntityInfo {

    private ViewInfo viewInfo = new ViewInfo();
    private List<DataInfo> dimensions = new ArrayList<>();
    private List<DataInfo> metrics = new ArrayList<>();
    private String entityId;

}
