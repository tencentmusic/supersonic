package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserWithPassword extends User {

    private String password;

    public UserWithPassword(Long id, String name, String displayName, String email, String password) {
        super(id, name, displayName, email);
        this.password = password;
    }

    public static UserWithPassword get(Long id, String name, String displayName, String email, String password) {
        return new UserWithPassword(id, name, displayName, email, password);
    }

}
