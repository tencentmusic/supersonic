package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_model_rela")
public class ModelRelaDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long domainId;

    private Long fromModelId;

    private Long toModelId;

    private String joinType;

    private String joinCondition;
}
