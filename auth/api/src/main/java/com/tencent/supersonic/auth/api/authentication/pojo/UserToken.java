package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {
    private Integer id;
    private String name;
    private String userName;
    private String token;
    private Long expireTime;
    private Date createDate;
    private Date expireDate;
}
