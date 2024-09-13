package com.tencent.supersonic.common.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_system_config")
public class SystemConfigDO {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String parameters;

    private String admin;
}
