package com.tencent.supersonic.common.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_sys_parameter")
public class SysParameterDO {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String parameters;

    private String admin;

}
