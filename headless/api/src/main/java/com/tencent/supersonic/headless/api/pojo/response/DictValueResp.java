package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.ToString;

/**
 * @author: kanedai
 * @date: 2024/6/22
 */
@ToString
@Data
public class DictValueResp {
    private String value;

    private String nature;

    private Long frequency;
}