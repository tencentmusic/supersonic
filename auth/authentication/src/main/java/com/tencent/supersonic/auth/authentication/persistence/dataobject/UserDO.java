package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户数据对象 对应表: s2_user
 */
@Data
@TableName("s2_user")
public class UserDO {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String name;

    /** 显示名称 */
    private String displayName;

    /** 密码 */
    private String password;

    /** 密码盐 */
    private String salt;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatarUrl;

    /** 是否系统管理员 */
    private Integer isAdmin;

    /** 状态: 1=启用, 0=禁用 */
    private Integer status;

    /** 最后登录时间 */
    private Date lastLogin;

    /** 工号 */
    private String employeeId;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    private Date createdAt;

    /** 创建人 */
    private String createdBy;

    /** 更新时间 */
    private Date updatedAt;

    /** 更新人 */
    private String updatedBy;

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

    /** @param phone */
    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }
}
