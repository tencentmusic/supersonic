package com.tencent.supersonic.auth.api.authentication.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserReq {

    @NotBlank(message = "name can not be null")
    private String name;

    @NotBlank(message = "password can not be null")
    private String password;

    @NotBlank(message = "password can not be null")
    private String newPassword;
}
