package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_user_token")
public class UserTokenDO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String name;
    String userName;
    Long expireTime;
    String token;
    String salt;
    Date createTime;
    Date updateTime;
    String createBy;
    String updateBy;
    Date expireDateTime;
}
