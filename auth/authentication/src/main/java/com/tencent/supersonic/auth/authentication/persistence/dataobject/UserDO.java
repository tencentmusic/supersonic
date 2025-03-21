package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("s2_user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** */
    private String name;

    /** */
    private String password;

    private String salt;

    /** */
    private String displayName;

    /** */
    private String email;

    /** */
    private Integer isAdmin;

    private Timestamp lastLogin;

    /** @param name */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /** @param password */
    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public void setSalt(String salt) {
        this.salt = salt == null ? null : salt.trim();
    }

    /** @param email */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

}
