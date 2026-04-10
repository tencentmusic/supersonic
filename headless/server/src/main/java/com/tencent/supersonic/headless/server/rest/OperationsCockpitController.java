package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.pojo.OperationsCockpitVO;
import com.tencent.supersonic.headless.server.service.OperationsCockpitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/cockpit")
@Slf4j
@RequiredArgsConstructor
public class OperationsCockpitController {

    private final OperationsCockpitService operationsCockpitService;

    @GetMapping
    public OperationsCockpitVO getCockpit(HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return operationsCockpitService.getCockpit(user);
    }
}
