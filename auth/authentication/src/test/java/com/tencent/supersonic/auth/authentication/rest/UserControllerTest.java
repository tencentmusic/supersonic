package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest {

    private UserController userController;

    private final AtomicReference<Long> assignedUserId = new AtomicReference<>();
    private final AtomicReference<List<Long>> assignedRoleIds = new AtomicReference<>();
    private final AtomicReference<String> assignedOperator = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        UserService userService =
                (UserService) Proxy.newProxyInstance(UserService.class.getClassLoader(),
                        new Class[] {UserService.class}, (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "getCurrentUser":
                                    return User.builder().id(100L).name("tenant_admin").build();
                                case "assignRolesToUser":
                                    assignedUserId.set((Long) args[0]);
                                    assignedRoleIds.set(toLongList(args[1]));
                                    assignedOperator.set((String) args[2]);
                                    return null;
                                default:
                                    return null;
                            }
                        });
        userController = new UserController(userService);
    }

    @Test
    void assignRolesToUserShouldPassUserRoleReqAndOperatorToService() {
        UserController.UserRoleReq req = new UserController.UserRoleReq();
        req.setUserId(88L);
        req.setRoleIds(List.of(1L, 2L));

        userController.assignRolesToUser(req, null, null);

        assertEquals(88L, assignedUserId.get());
        assertEquals(List.of(1L, 2L), assignedRoleIds.get());
        assertEquals("tenant_admin", assignedOperator.get());
    }

    private List<Long> toLongList(Object value) {
        if (!(value instanceof List<?> source)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Long longValue) {
                result.add(longValue);
            }
        }
        return result;
    }
}
