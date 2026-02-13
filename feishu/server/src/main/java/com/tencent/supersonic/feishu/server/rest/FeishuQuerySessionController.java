package com.tencent.supersonic.feishu.server.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.service.FeishuUserMappingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feishu/querySessions")
@RequiredArgsConstructor
@Slf4j
public class FeishuQuerySessionController {

    private final FeishuUserMappingService mappingService;

    @GetMapping
    public IPage<FeishuQuerySessionDO> list(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return mappingService.listSessions(current, pageSize, status, startDate, endDate);
    }
}
