package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.ActionInfo;
import com.tencent.supersonic.headless.api.pojo.RuleInfo;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.QueryRuleType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class QueryRuleReq extends SchemaItem {

    /** dataSetID */
    private Long dataSetId;

    /** 规则的优先级, 1-低,2-中,3-高 */
    private Integer priority = 1;

    /** 规则类型 */
    @NotNull
    private QueryRuleType ruleType;

    /** 具体规则信息 */
    @NotNull
    private RuleInfo rule;

    /** 规则输出信息 */
    private ActionInfo action;

    /** 扩展信息 */
    private Map<String, String> ext = new HashMap<>();
}
