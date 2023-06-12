package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import java.util.List;
import lombok.Data;

@Data
public class EntityRichInfo {

    private Long domainId;
    private String domainName;
    private String domainBizName;

    private List<String> names;

    private List<DimSchemaResp> entityIds;

    private EntityInternalDetail entityInternalDetailDesc;

}