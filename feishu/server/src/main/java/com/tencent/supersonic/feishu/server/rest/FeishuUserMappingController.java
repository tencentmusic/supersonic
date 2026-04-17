package com.tencent.supersonic.feishu.server.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.service.FeishuUserMappingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/feishu/userMappings")
@RequiredArgsConstructor
@Slf4j
public class FeishuUserMappingController {

    private final FeishuUserMappingService mappingService;

    private void assertAdmin(User user) {
        if (user == null || (!user.isSuperAdmin()
                && (user.getIsAdmin() == null || user.getIsAdmin() != 1))) {
            throw new InvalidPermissionException("仅管理员可管理飞书用户映射");
        }
    }

    @GetMapping
    public IPage<FeishuUserMappingDO> list(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        assertAdmin(UserHolder.findUser(request, response));
        return mappingService.listMappings(current, pageSize);
    }

    @GetMapping("/{id}")
    public FeishuUserMappingDO getById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        assertAdmin(UserHolder.findUser(request, response));
        return mappingService.getMappingById(id);
    }

    @PostMapping
    public FeishuUserMappingDO create(@RequestBody FeishuUserMappingDO mapping,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        mapping.setTenantId(user.getTenantId());
        return mappingService.createMapping(mapping);
    }

    @PatchMapping("/{id}")
    public FeishuUserMappingDO update(@PathVariable Long id,
            @RequestBody FeishuUserMappingDO mapping, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        mapping.setId(id);
        mapping.setTenantId(user.getTenantId());
        return mappingService.updateMapping(mapping);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        mappingService.deleteMapping(id);
    }

    @PostMapping("/{id}:enable")
    public void enable(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        mappingService.toggleStatus(id, 1);
    }

    @PostMapping("/{id}:disable")
    public void disable(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        mappingService.toggleStatus(id, 0);
    }
}
