package com.tencent.supersonic.auth.authorization.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/queryGroup")
    public List<AuthGroup> queryAuthGroup(@RequestParam("modelId") String modelId,
            @RequestParam(value = "groupId", required = false) Integer groupId) {
        return authService.queryAuthGroups(modelId, groupId);
    }

    /**
     * 新建权限组
     */
    @PostMapping("/createGroup")
    public void newAuthGroup(@RequestBody AuthGroup group) {
        group.setGroupId(null);
        authService.addOrUpdateAuthGroup(group);
    }

    @PostMapping("/removeGroup")
    public void removeAuthGroup(@RequestBody AuthGroup group) {
        authService.removeAuthGroup(group);
    }

    /**
     * 更新权限组
     *
     * @param group
     */
    @PostMapping("/updateGroup")
    public void updateAuthGroup(@RequestBody AuthGroup group) {
        if (group.getGroupId() == null || group.getGroupId() == 0) {
            throw new RuntimeException("groupId is empty");
        }
        authService.addOrUpdateAuthGroup(group);
    }

    /**
     * 查询有权限访问的受限资源id
     *
     * @param req
     * @return
     */
    @PostMapping("/queryAuthorizedRes")
    public AuthorizedResourceResp queryAuthorizedResources(@RequestBody QueryAuthResReq req,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return authService.queryAuthorizedResources(req, user);
    }
}
