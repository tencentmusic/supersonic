package com.tencent.supersonic.chat.api.pojo.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class EntityInfo {

    private ModelInfo modelInfo = new ModelInfo();
    private List<DataInfo> dimensions = new ArrayList<>();
    private List<DataInfo> metrics = new ArrayList<>();
    private String entityId;
}
