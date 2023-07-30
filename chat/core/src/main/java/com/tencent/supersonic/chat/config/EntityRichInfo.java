package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;

import java.util.List;
import lombok.Data;

@Data
public class EntityRichInfo {
    /**
     *  entity alias
     */
    private List<String> names;

    private SchemaElement dimItem;
}
