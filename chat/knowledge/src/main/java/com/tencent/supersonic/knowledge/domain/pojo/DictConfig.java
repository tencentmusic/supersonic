package com.tencent.supersonic.knowledge.domain.pojo;

import java.util.List;
import lombok.Data;


@Data
public class DictConfig {

    private Long domainId;

    private List<DimValueInfo> dimValueInfoList;
}