package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserWithPassword extends User {

    private String password;

    public UserWithPassword(Long id, String name, String displayName, String email, String password, Integer isAdmin) {
        super(id, name, displayName, email, isAdmin);
        this.password = password;
    }

    public static UserWithPassword get(Long id, String name, String displayName,
                                       String email, String password, Integer isAdmin) {
        return new UserWithPassword(id, name, displayName, email, password, isAdmin);
    }

}
