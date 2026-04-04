package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.PermissionDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.PermissionDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserDOMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionServiceImplTest {

    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        PermissionDO adminPermission = new PermissionDO();
        adminPermission.setCode("platform:all");
        adminPermission.setStatus(1);
        adminPermission.setSortOrder(1);

        PermissionDO normalPermission = new PermissionDO();
        normalPermission.setCode("tenant:read");
        normalPermission.setStatus(1);
        normalPermission.setSortOrder(1);

        PermissionDOMapper permissionDOMapper = (PermissionDOMapper) Proxy.newProxyInstance(
                PermissionDOMapper.class.getClassLoader(), new Class[] {PermissionDOMapper.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "selectList":
                            return List.of(adminPermission);
                        case "selectByUserId":
                            return List.of(normalPermission);
                        default:
                            return null;
                    }
                });

        UserDOMapper userDOMapper =
                (UserDOMapper) Proxy.newProxyInstance(UserDOMapper.class.getClassLoader(),
                        new Class[] {UserDOMapper.class}, (proxy, method, args) -> {
                            if (!"selectById".equals(method.getName())) {
                                return null;
                            }
                            Long userId = (Long) args[0];
                            if (userId == 1L) {
                                UserDO admin = new UserDO();
                                admin.setId(1L);
                                admin.setIsAdmin(1);
                                return admin;
                            }
                            if (userId == 2L) {
                                UserDO normal = new UserDO();
                                normal.setId(2L);
                                normal.setIsAdmin(0);
                                return normal;
                            }
                            return null;
                        });

        permissionService = new PermissionServiceImpl(permissionDOMapper, userDOMapper);
    }

    @Test
    void getPermissionCodesByUserIdShouldReturnAllPermissionsForAdmin() {
        List<String> codes = permissionService.getPermissionCodesByUserId(1L);
        assertEquals(List.of("platform:all"), codes);
    }

    @Test
    void getPermissionCodesByUserIdShouldReturnUserPermissionsForNormalUser() {
        List<String> codes = permissionService.getPermissionCodesByUserId(2L);
        assertEquals(List.of("tenant:read"), codes);
    }

    @Test
    void getPermissionCodesByUserIdShouldReturnEmptyWhenUserNotFound() {
        List<String> codes = permissionService.getPermissionCodesByUserId(999L);
        assertTrue(codes.isEmpty());
    }
}
