package com.tencent.supersonic.chat.api.pojo;

import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class Filter {

    private String bizName;

    private String name;

    private FilterOperatorEnum operator = FilterOperatorEnum.EQUALS;

    private Object value;

    private Long elementID;
}