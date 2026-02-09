package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DataSetAuthGroup;
import com.tencent.supersonic.headless.server.service.DataSetAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semantic/dataset/auth")
@RequiredArgsConstructor
public class DataSetAuthController {

    private final DataSetAuthService dataSetAuthService;

    @GetMapping("/queryGroups")
    public List<DataSetAuthGroup> queryAuthGroups(@RequestParam("datasetId") Long datasetId,
            @RequestParam(value = "groupId", required = false) Long groupId) {
        return dataSetAuthService.queryAuthGroups(datasetId, groupId);
    }

    @PostMapping("/createGroup")
    public DataSetAuthGroup createAuthGroup(@RequestBody DataSetAuthGroup group,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dataSetAuthService.createAuthGroup(group, user);
    }

    @PostMapping("/updateGroup")
    public void updateAuthGroup(@RequestBody DataSetAuthGroup group, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        if (group.getGroupId() == null) {
            throw new RuntimeException("groupId is required for update");
        }
        dataSetAuthService.updateAuthGroup(group, user);
    }

    @DeleteMapping("/removeGroup/{groupId}")
    public void removeAuthGroup(@PathVariable("groupId") Long groupId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        dataSetAuthService.removeAuthGroup(groupId, user);
    }

    @GetMapping("/queryAuthorizedRes")
    public AuthorizedResourceResp queryAuthorizedResources(
            @RequestParam("datasetId") Long datasetId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dataSetAuthService.queryAuthorizedResources(datasetId, user);
    }

    @GetMapping("/checkPermission")
    public Map<String, Boolean> checkPermission(@RequestParam("datasetId") Long datasetId,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        Map<String, Boolean> result = new HashMap<>();
        result.put("hasViewPermission",
                dataSetAuthService.checkDataSetViewPermission(datasetId, user));
        result.put("hasAdminPermission",
                dataSetAuthService.checkDataSetAdminPermission(datasetId, user));
        return result;
    }

    @GetMapping("/rowFilters")
    public List<String> getRowFilters(@RequestParam("datasetId") Long datasetId,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dataSetAuthService.getRowFilters(datasetId, user);
    }
}
