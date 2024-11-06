package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private Long id;

    private String name;

    private String displayName;

    private String email;

    private Integer isAdmin;

    public static User get(Long id, String name, String displayName, String email,
            Integer isAdmin) {
        return new User(id, name, displayName, email, isAdmin);
    }

    public static User get(Long id, String name) {
        return new User(id, name, name, name, 0);
    }

    public static User getDefaultUser() {
        return new User(1L, "admin", "admin", "admin@email", 1);
    }

    public static User getVisitUser() {
        return new User(1L, "visit", "visit", "visit@email", 0);
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
