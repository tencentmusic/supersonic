package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinCondition {

    private String leftField;

    private String rightField;

    private FilterOperatorEnum operator;
}
