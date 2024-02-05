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

    private Integer isAdmin;

    public static User get(Long id, String name, String displayName, String email, Integer isAdmin) {
        return new User(id, name, displayName, email, isAdmin);
    }

    public static User get(Long id, String name) {
        return new User(id, name, name, name, 0);
    }

    public static User getFakeUser() {
        return new User(1L, "admin", "admin", "admin@email", 1);
    }

    public static User getAppUser(int appId) {
        String name = String.format("app_%s", appId);
        return new User(1L, name, name, "", 1);
    }

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? name : displayName;
    }

    public boolean isSuperAdmin() {
        return isAdmin != null && isAdmin == 1;
    }

}
