package com.tencent.supersonic.chat.api.pojo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class EntityInfo {

    private DomainInfo domainInfo = new DomainInfo();
    private List<DataInfo> dimensions = new ArrayList<>();
    private List<DataInfo> metrics = new ArrayList<>();
    private String entityId;
}
