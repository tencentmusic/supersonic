package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.request.UserTokenReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public void register(@RequestBody UserReq userCmd) {
        userService.register(userCmd);
    }

    @PostMapping("/login")
    public String login(@RequestBody UserReq userCmd, HttpServletRequest request) {
        return userService.login(userCmd, request);
    }

    @PostMapping("/resetPassword")
    public void resetPassword(@RequestBody UserReq userCmd, HttpServletRequest request,
            HttpServletResponse response) {
        User user = userService.getCurrentUser(request, response);
        userService.resetPassword(user.getName(), userCmd.getPassword(), userCmd.getNewPassword());
    }

    @PostMapping("/generateToken")
    public UserToken generateToken(@RequestBody UserTokenReq userTokenReq,
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
}
