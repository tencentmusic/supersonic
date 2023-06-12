package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String name;

    private String displayName;

    private String email;

    public static User get(Long id, String name, String displayName, String email) {
        return new User(id, name, displayName, email);
    }

    public static User getFakeUser() {
        return new User(1L, "admin", "admin", "admin@email");
    }

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? name : displayName;
    }


}
