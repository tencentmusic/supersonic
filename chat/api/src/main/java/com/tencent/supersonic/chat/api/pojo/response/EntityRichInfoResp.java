package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;

import java.util.List;
import lombok.Data;

@Data
public class EntityRichInfoResp {
    /**
     *  entity alias
     */
    private List<String> names;

    private SchemaElement dimItem;
}
