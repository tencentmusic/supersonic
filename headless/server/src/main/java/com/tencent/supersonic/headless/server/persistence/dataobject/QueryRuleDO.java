package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_query_rule")
public class QueryRuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** dataSetID */
    private Long dataSetId;

    /** 规则的优先级, 0-系统默认规则 */
    private Integer priority;

    /** 规则类型 */
    private String ruleType;

    /** 规则名称 */
    private String name;

    /** 规则业务名称 */
    private String bizName;

    /** 描述 */
    private String description;

    /** 具体规则信息 */
    private String rule;

    /** 规则输出信息 */
    private String action;

    /** 状态,0-正常,1-下线,2-删除 */
    private Integer status;

    /** 创建时间 */
    private Date createdAt;

    /** 创建人 */
    private String createdBy;

    /** 更新时间 */
    private Date updatedAt;

    /** 更新人 */
    private String updatedBy;

    /** 扩展信息 */
    private String ext;
}
