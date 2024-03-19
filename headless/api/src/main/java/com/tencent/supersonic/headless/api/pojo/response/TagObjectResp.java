package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString(callSuper = true)
public class TagObjectResp extends SchemaItem {

    /**
     * 关联到某个主题域下
     */
    private Long domainId;

    /**
     * 扩展信息
     */
    private Map<String, String> ext;
}