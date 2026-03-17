package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.request.UserTokenReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/auth/user")
@Slf4j
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/getCurrentUser")
    public User getCurrentUser(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        return userService.getCurrentUser(httpServletRequest, httpServletResponse);
    }

    @GetMapping("/{userId}")
    public User getUserById(@PathVariable("userId") Long userId) {
        return userService.getUserById(userId);
    }

    @GetMapping("/getUserNames")
    public List<String> getUserNames() {
        return userService.getUserNames();
    }

    @GetMapping("/getUserList")
    public List<User> getUserList() {
        return userService.getUserList();
    }

    @GetMapping("/getOrganizationTree")
    public List<Organization> getOrganizationTree() {
        return userService.getOrganizationTree();
    }

    @GetMapping("/getUserAllOrgId/{userName}")
    public Set<String> getUserAllOrgId(@PathVariable("userName") String userName) {
        return userService.getUserAllOrgId(userName);
    }

    @GetMapping("/getUserByOrg/{org}")
    public List<User> getUserByOrg(@PathVariable("org") String org) {
        return userService.getUserByOrg(org);
    }

    @PostMapping("/register")
    public void register(@Valid @RequestBody UserReq userCmd) {
        userService.register(userCmd);
    }

    @DeleteMapping("/delete/{userId}")
    public void delete(@PathVariable("userId") long userId, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws IllegalAccessException {
        User user = userService.getCurrentUser(httpServletRequest, httpServletResponse);
        if (user.getIsAdmin() != 1) {
            throw new IllegalAccessException("only admin can delete user");
        }
        userService.deleteUser(userId);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody UserReq userCmd, HttpServletRequest request) {
        return userService.login(userCmd, request);
    }

    @PostMapping("/resetPassword")
    public void resetPassword(@Valid @RequestBody UserReq userCmd, HttpServletRequest request,
            HttpServletResponse response) {
        if (userCmd.getNewPassword() == null || userCmd.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("newPassword can not be null");
        }
        User user = userService.getCurrentUser(request, response);
        userService.resetPassword(user.getName(), userCmd.getPassword(), userCmd.getNewPassword());
    }

    @PostMapping("/generateToken")
    public UserToken generateToken(@Valid @RequestBody UserTokenReq userTokenReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = userService.getCurrentUser(request, response);
        return userService.generateToken(userTokenReq.getName(), user.getName(),
                userTokenReq.getExpireTime());
    }

    @GetMapping("/getUserTokens")
    public List<UserToken> getUserTokens(HttpServletRequest request, HttpServletResponse response) {
        User user = userService.getCurrentUser(request, response);
        return userService.getUserTokens(user.getName());
    }

    @GetMapping("/getUserToken")
    public UserToken getUserToken(@RequestParam(name = "tokenId") Long tokenId) {
        return userService.getUserToken(tokenId);
    }

    @PostMapping("/deleteUserToken")
    public void deleteUserToken(@RequestParam(name = "tokenId") Long tokenId) {
        userService.deleteUserToken(tokenId);
    }

    @PostMapping("/role")
    public void assignRolesToUser(@RequestBody UserRoleReq userRoleReq, HttpServletRequest request,
            HttpServletResponse response) {
        User currentUser = userService.getCurrentUser(request, response);
        userService.assignRolesToUser(userRoleReq.getUserId(), userRoleReq.getRoleIds(),
                currentUser.getName());
    }

    @GetMapping("/{userId}/role-ids")
    public List<Long> getUserRoleIds(@PathVariable("userId") Long userId) {
        return userService.getUserRoleIds(userId);
    }

    @Data
    public static class UserRoleReq {
        private Long userId;
        private List<Long> roleIds;
    }
}
