package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import java.util.List;
import lombok.Data;

@Data
public class EntityRichInfo {

//    private Long domainId;
//    private String domainName;
//    private String domainBizName;

    /**
     *  entity alias
     */
    private List<String> names;

    private DimSchemaResp dimItem;
}