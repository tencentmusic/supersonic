package com.tencent.supersonic.auth.authorization.rest;

import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.authorization.application.AuthApplicationService;
import com.tencent.supersonic.auth.authorization.domain.pojo.AuthGroup;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
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

    private final AuthApplicationService service;

    public AuthController(AuthApplicationService service) {
        this.service = service;
    }

    @GetMapping("/queryGroup")
    public List<AuthGroup> queryAuthGroup(@RequestParam("domainId") String domainId,
            @RequestParam(value = "groupId", required = false) Integer groupId) {
        return service.queryAuthGroups(domainId, groupId);
    }

    /**
     * 新建权限组
     */
    @PostMapping("/createGroup")
    public void newAuthGroup(@RequestBody AuthGroup group) {
        group.setGroupId(null);
        service.updateAuthGroup(group);
    }

    @PostMapping("/removeGroup")
    public void removeAuthGroup(@RequestBody AuthGroup group) {
        service.removeAuthGroup(group);
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
        service.updateAuthGroup(group);
    }

    /**
     * 查询有权限访问的受限资源id
     *
     * @param req
     * @param request
     * @return
     */
    @PostMapping("/queryAuthorizedRes")
    public AuthorizedResourceResp queryAuthorizedResources(@RequestBody QueryAuthResReq req,
            HttpServletRequest request) {
        return service.queryAuthorizedResources(req, request);
    }
}
