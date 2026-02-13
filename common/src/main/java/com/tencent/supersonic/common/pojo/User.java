package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    private Long id;

    private String name;

    private String displayName;

    private String email;

    private String phone;

    private String employeeId;

    private Integer isAdmin;

    private Timestamp lastLogin;

    /** Tenant ID for multi-tenancy */
    private Long tenantId;

    /** User role within tenant */
    private String role;

    /** User role ID */
    private Long roleId;

    /** User permissions (for menu control) */
    private List<String> permissions;

    /** User status: 1=enabled, 0=disabled */
    private Integer status;

    /** Primary organization ID */
    private Long organizationId;

    /** Primary organization name */
    private String organizationName;

    /** Role IDs */
    private List<Long> roleIds;

    /** Role names */
    private List<String> roleNames;

    /**
     * Get default tenant ID from TenantConfig. Falls back to 1L if Spring context is not ready.
     */
    private static Long getDefaultTenantId() {
        try {
            return ContextUtils.getBean(TenantConfig.class).getDefaultTenantId();
        } catch (Exception e) {
            return 1L;
        }
    }

    public static User get(Long id, String name, String displayName, String email,
            Integer isAdmin) {
        return User.builder().id(id).name(name).displayName(displayName).email(email)
                .isAdmin(isAdmin).tenantId(getDefaultTenantId()).role("USER").build();
    }

    public static User get(Long id, String name, String displayName, String email, Integer isAdmin,
            Long tenantId, String role) {
        return User.builder().id(id).name(name).displayName(displayName).email(email)
                .isAdmin(isAdmin).tenantId(tenantId).role(role).build();
    }

    public static User get(Long id, String name) {
        return User.builder().id(id).name(name).displayName(name).email(name).isAdmin(0)
                .tenantId(0L).role("VISITOR").build();
    }

    /** @deprecated Use {@code UserService.getDefaultUser()} instead. */
    @Deprecated
    public static User getDefaultUser() {
        return User.builder().id(1L).name("admin").displayName("admin").email("admin@email")
                .isAdmin(1).tenantId(getDefaultTenantId()).role("ADMIN").build();
    }

    /** @deprecated Use {@code UserService.getVisitUser()} instead. */
    @Deprecated
    public static User getVisitUser() {
        return User.builder().id(null).name("visit").displayName("visit").email("visit@email")
                .isAdmin(0).tenantId(0L).role("VISITOR").build();
    }

    /** @deprecated Use {@code UserService.getAppUser(int)} instead. */
    @Deprecated
    public static User getAppUser(int appId) {
        String name = String.format("app_%s", appId);
        return User.builder().id(1L).name(name).displayName(name).email("").isAdmin(1)
                .tenantId(getDefaultTenantId()).role("APP").build();
    }

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? name : displayName;
    }

    public boolean isSuperAdmin() {
        return isAdmin != null && isAdmin == 1;
    }
}
