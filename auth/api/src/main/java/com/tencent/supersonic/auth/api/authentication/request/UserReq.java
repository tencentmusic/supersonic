package com.tencent.supersonic.auth.api.authentication.request;


import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserReq {

    @NotBlank(message = "name can not be null")
    private String name;

    @NotBlank(message = "password can not be null")
    private String password;


}
