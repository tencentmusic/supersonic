package com.tencent.supersonic.chat.core.knowledge;

import java.util.List;
import lombok.Data;


@Data
public class DictConfig {

    private Long modelId;

    private List<DimValueInfo> dimValueInfoList;
}