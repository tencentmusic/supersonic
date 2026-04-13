package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level test for {@link ReportDeliveryController}, focusing on the custom verb routes
 * used by the delivery config page (e.g. `/configs/{id}:test`). Uses MockMvc standalone setup with
 * the `${spring.servlet.api-path:/api}` placeholder resolved to `/api`.
 *
 * <p>
 * Auth is bypassed by injecting a stub {@link UserStrategy} via {@link UserHolder#setStrategy} and
 * a mock {@link SystemConfigService} via {@link ContextUtils} (the project disables Mockito's
 * inline mock maker so {@code mockStatic} is unavailable).
 */
class ReportDeliveryControllerTest {

    private ReportDeliveryService deliveryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        deliveryService = mock(ReportDeliveryService.class);
        ReportDeliveryController controller = new ReportDeliveryController(deliveryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("spring.servlet.api-path", "/api").build();

        User admin = new User();
        admin.setName("admin");
        admin.setIsAdmin(1);

        UserStrategy strategy = mock(UserStrategy.class);
        when(strategy.findUser(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(admin);
        UserHolder.setStrategy(strategy);

        SystemConfig sysConfig = new SystemConfig();
        sysConfig.setAdmins(new ArrayList<>());
        SystemConfigService sysConfigService = mock(SystemConfigService.class);
        when(sysConfigService.getSystemConfig()).thenReturn(sysConfig);

        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(SystemConfigService.class)).thenReturn(sysConfigService);
        new ContextUtils().setApplicationContext(ctx);
    }

    @Test
    void testConfigEndpointShouldRouteAndReturnNonEmptyBody() throws Exception {
        ReportDeliveryRecordDO record = new ReportDeliveryRecordDO();
        record.setId(123L);
        when(deliveryService.testDelivery(2L)).thenReturn(record);

        MvcResult result = mockMvc.perform(post("/api/semantic/delivery/configs/{id}:test", 2L))
                .andDo(print()).andExpect(status().isOk()).andReturn();

        verify(deliveryService, times(1)).testDelivery(2L);

        // Regression guard: a previous void return produced an empty HTTP body, which
        // caused the frontend umi-request JSON parser to fail silently, leaving the
        // "测试" button with no feedback. A non-null return value ensures Spring writes
        // a parseable body (which ResponseAdvice wraps into ResultData in the real app).
        String body = result.getResponse().getContentAsString();
        assertFalse(body.isEmpty(), "response body must not be empty");
    }

    @Test
    void testConfigEndpointShouldExtractPathVariableCorrectly() throws Exception {
        mockMvc.perform(post("/api/semantic/delivery/configs/99:test")).andExpect(status().isOk());

        verify(deliveryService, times(1)).testDelivery(99L);
    }
}
