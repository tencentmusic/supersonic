package com.tencent.supersonic.feishu.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_feishu_user_mapping")
public class FeishuUserMappingDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String feishuOpenId;
    private String feishuUnionId;
    private String feishuUserName;
    private String feishuEmail;
    private String feishuMobile;
    private String feishuEmployeeId;
    private Long s2UserId;
    private Long tenantId;
    private Integer defaultAgentId;
    private String matchType;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
